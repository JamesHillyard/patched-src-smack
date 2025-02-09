/**
 *
 * Copyright 2005-2007 Jive Software.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jivesoftware.smackx.commands.provider;

import java.io.IOException;

import org.jivesoftware.smack.packet.IqData;
import org.jivesoftware.smack.packet.StanzaError;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smack.provider.IqProvider;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;

import org.jivesoftware.smackx.commands.AdHocCommand;
import org.jivesoftware.smackx.commands.AdHocCommand.Action;
import org.jivesoftware.smackx.commands.AdHocCommandNote;
import org.jivesoftware.smackx.commands.packet.AdHocCommandData;
import org.jivesoftware.smackx.xdata.provider.DataFormProvider;

/**
 * The AdHocCommandDataProvider parses AdHocCommandData packets.
 *
 * @author Gabriel Guardincerri
 */
public class AdHocCommandDataProvider extends IqProvider<AdHocCommandData> {

    @Override
    public AdHocCommandData parse(XmlPullParser parser, int initialDepth, IqData iqData, XmlEnvironment xmlEnvironment) throws XmlPullParserException, IOException, SmackParsingException {
        boolean done = false;
        AdHocCommandData adHocCommandData = new AdHocCommandData();
        DataFormProvider dataFormProvider = new DataFormProvider();

        XmlPullParser.Event eventType;
        String elementName;
        String namespace;
        adHocCommandData.setSessionID(parser.getAttributeValue("", "sessionid"));
        adHocCommandData.setNode(parser.getAttributeValue("", "node"));

        // Status
        String status = parser.getAttributeValue("", "status");
        if (AdHocCommand.Status.executing.toString().equalsIgnoreCase(status)) {
            adHocCommandData.setStatus(AdHocCommand.Status.executing);
        }
        else if (AdHocCommand.Status.completed.toString().equalsIgnoreCase(status)) {
            adHocCommandData.setStatus(AdHocCommand.Status.completed);
        }
        else if (AdHocCommand.Status.canceled.toString().equalsIgnoreCase(status)) {
            adHocCommandData.setStatus(AdHocCommand.Status.canceled);
        }

        // Action
        String action = parser.getAttributeValue("", "action");
        if (action != null) {
            Action realAction = AdHocCommand.Action.valueOf(action);
            if (realAction == null || realAction.equals(Action.unknown)) {
                adHocCommandData.setAction(Action.unknown);
            }
            else {
                adHocCommandData.setAction(realAction);
            }
        }
        while (!done) {
            eventType = parser.next();
            namespace = parser.getNamespace();
            if (eventType == XmlPullParser.Event.START_ELEMENT) {
                elementName = parser.getName();
                if (parser.getName().equals("actions")) {
                    String execute = parser.getAttributeValue("", "execute");
                    if (execute != null) {
                        adHocCommandData.setExecuteAction(AdHocCommand.Action.valueOf(execute));
                    }
                }
                else if (parser.getName().equals("next")) {
                    adHocCommandData.addAction(AdHocCommand.Action.next);
                }
                else if (parser.getName().equals("complete")) {
                    adHocCommandData.addAction(AdHocCommand.Action.complete);
                }
                else if (parser.getName().equals("prev")) {
                    adHocCommandData.addAction(AdHocCommand.Action.prev);
                }
                else if (elementName.equals("x") && namespace.equals("jabber:x:data")) {
                    adHocCommandData.setForm(dataFormProvider.parse(parser));
                }
                else if (parser.getName().equals("note")) {
                    String typeString = parser.getAttributeValue("", "type");
                    AdHocCommandNote.Type type;
                    if (typeString != null) {
                        type = AdHocCommandNote.Type.valueOf(typeString);
                    } else {
                        // Type is optional and 'info' if not present.
                        type = AdHocCommandNote.Type.info;
                    }
                    String value = parser.nextText();
                    adHocCommandData.addNote(new AdHocCommandNote(type, value));
                }
                else if (parser.getName().equals("error")) {
                    StanzaError error = PacketParserUtils.parseError(parser);
                    adHocCommandData.setError(error);
                }
            }
            else if (eventType == XmlPullParser.Event.END_ELEMENT) {
                if (parser.getName().equals("command")) {
                    done = true;
                }
            }
        }
        return adHocCommandData;
    }

    public static class BadActionError extends ExtensionElementProvider<AdHocCommandData.SpecificError> {
        @Override
        public AdHocCommandData.SpecificError parse(XmlPullParser parser, int initialDepth, XmlEnvironment xmlEnvironment)  {
            return new AdHocCommandData.SpecificError(AdHocCommand.SpecificErrorCondition.badAction);
        }
    }

    public static class MalformedActionError extends ExtensionElementProvider<AdHocCommandData.SpecificError> {
        @Override
        public AdHocCommandData.SpecificError parse(XmlPullParser parser, int initialDepth, XmlEnvironment xmlEnvironment)  {
            return new AdHocCommandData.SpecificError(AdHocCommand.SpecificErrorCondition.malformedAction);
        }
    }

    public static class BadLocaleError extends ExtensionElementProvider<AdHocCommandData.SpecificError> {
        @Override
        public AdHocCommandData.SpecificError parse(XmlPullParser parser, int initialDepth, XmlEnvironment xmlEnvironment)  {
            return new AdHocCommandData.SpecificError(AdHocCommand.SpecificErrorCondition.badLocale);
        }
    }

    public static class BadPayloadError extends ExtensionElementProvider<AdHocCommandData.SpecificError> {
        @Override
        public AdHocCommandData.SpecificError parse(XmlPullParser parser, int initialDepth, XmlEnvironment xmlEnvironment)  {
            return new AdHocCommandData.SpecificError(AdHocCommand.SpecificErrorCondition.badPayload);
        }
    }

    public static class BadSessionIDError extends ExtensionElementProvider<AdHocCommandData.SpecificError> {
        @Override
        public AdHocCommandData.SpecificError parse(XmlPullParser parser, int initialDepth, XmlEnvironment xmlEnvironment)  {
            return new AdHocCommandData.SpecificError(AdHocCommand.SpecificErrorCondition.badSessionid);
        }
    }

    public static class SessionExpiredError extends ExtensionElementProvider<AdHocCommandData.SpecificError> {
        @Override
        public AdHocCommandData.SpecificError parse(XmlPullParser parser, int initialDepth, XmlEnvironment xmlEnvironment)  {
            return new AdHocCommandData.SpecificError(AdHocCommand.SpecificErrorCondition.sessionExpired);
        }
    }
}

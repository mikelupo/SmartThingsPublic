/**
 *  Furnace On The Fritz
 *
 *  Copyright 2015 michael lupo
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *  Credit to the author of this post: http://community.smartthings.com/t/high-temperature-notify/520/2 
 *  for inspiration.
 */
definition(
    name: "Furnace On The Fritz",
    namespace: "mikelupo",
    author: "michael lupo",
    description: "If the furnace has shutdown due to an error condition, it will close a relay. We detect this closure and generate an event to the ST cloud that 'hopefully' will send you a push letting you know the furnace has sh** the bed. ",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("When this relay is closed...") {
        input "furnaceAlarmContact", "capability.contactSensor", title: "Select the furnace shield"
	}
	
    section("Temperature Sensors") {
		input "exSensor", "capability.temperatureMeasurement", title: "Choose Outdoor Sensor"
    }

    section("Notify me") {
        input "phone1", "phone", title: "Phone Number (Optional.  Push Notification will be sent in addition)", required: false
        input "freq", "decimal", title: "Frequency of notifications (In minutes.  Only one notification will be sent if left blank.)", required: false
    }
}

def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}

def initialize() {
	//subscribe to attributes, devices, locations, etc.
    
    //when the contact is open, the furnace should be happy 
    //(again) and we should unschedule any events.
    subscribe furnaceAlarmContact, "contact.open", eFurnaceHandlerUnschedule
    
    //when the contact is closed, I think this is when the furnace is on the fritz
    subscribe furnaceAlarmContact, "contact.closed", eFurnaceHandlerSchedule
    subscribe exSensor, "temperature", eFurnaceHandler
}

// TODO: implement event handlers
def openHandler(evt) {
	log.debug ("got inside openHandler")
}

def closedHandler(evt) {
	log.debug ("got inside closedHandler")
}

def sendNotif() {
    def exTemp = exSensor.latestValue("temperature")
    log.debug exTemp
	
    sendPush("Furnace Error occurred. FYI, the outside temperature is $exTemp degrees.")
    if (phone1) {
        sendSms(phone1, "Furnace Error occurred. FYI, the outside temperature is $exTemp degrees")
    }
    
}

def sendOKNotif() {
    def exTemp = exSensor.latestValue("temperature")
    log.debug("Furnace Corrected")
    sendPush("Furnace corrected. FYI, the outside temperature is $exTemp degrees.")
    if (phone1) {
        sendSms(phone1, "Furnace corrected. FYI, the outside temperature is $exTemp degrees")
    }
    
}

def eFurnaceHandlerSchedule(evt) {
    //if we set a frequency of notification, schedule that. 
    log.debug("Furnace Error Detected")
    sendNotif() //do it the first time, then use the CRON if freq defined.
    if(freq) {
        schedule("0 0/$freq * * * ?", sendNotif)
        //else, just send one notification and be done with it.
    } 
    else {
        sendNotif()
    }
}

def eFurnaceHandlerUnschedule(evt) {
    sendOKNotif()
    unschedule()
    unschedule("sendNotif")
}
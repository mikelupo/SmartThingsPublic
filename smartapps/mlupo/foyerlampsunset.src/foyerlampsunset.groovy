/**
 *  foyerLampSunset
 *
 *  Copyright 2014 michael lupo
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
 */
definition(
    name: "foyerLampSunset",
    namespace: "mlupo",
    author: "michael lupo",
    description: "Turn the foyer lamp on 1.5 hours before sunset and leave on till 11:59pm.",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section ("At sunset...") {
		input "sunsetMode", "mode", title: "Change mode to?", required: false
		input "foyerOutlet", "capability.switch", title: "Turn on?", required: false, multiple: true
	}
    
    section ("Sunset offset (optional)...") {
		input "sunsetOffsetValue", "text", title: "HH:MM", required: false
		input "sunsetOffsetDir", "enum", title: "Before or After", required: false, options: ["Before","After"]
	}
    
    //section ("How long to leave the light on for? MINUTES (ie. 90)") {
	//	input name: "minutes", title: "Minutes?", type: "number", multiple: false
	//}
    
    section("What time to turn the light off?") {
    	input "time1", "time", title: "When?", required: true
    }
    
    section ("Zip code (optional, defaults to location coordinates)...") {
		input "zipCode", "text", required: false
	}
	section( "Notifications" ) {
		input "sendPushMessage", "enum", title: "Send a push notification?", options: ["Yes", "No"], required: false
		input "phoneNumber", "phone", title: "Send a text message?", required: false
	}
}

def installed() {
	initialize()
}

def updated() {
	unsubscribe()
	//unschedule handled in astroCheck method
	initialize()
}

def initialize() {
	subscribe(location, "position", locationPositionChange)
	subscribe(location, "sunsetTime", sunriseSunsetTimeHandler)
	
	astroCheck()
}

def locationPositionChange(evt) {
	log.trace "locationChange()"
	astroCheck()
}

def sunriseSunsetTimeHandler(evt) {
	log.trace "sunriseSunsetTimeHandler()"
	astroCheck()
}


def astroCheck() {
    log.debug "sunsetOffset is: $sunsetOffset"
	def s = getSunriseAndSunset(zipCode: zipCode, sunriseOffset: sunriseOffset, sunsetOffset: sunsetOffset)
    def off = getSunriseAndSunset(zipCode: zipCode, sunriseOffset: sunriseOffset, sunsetOffset: sunsetOffset)
	def now = new Date()
	def sunsetTime = s.sunset
    
	log.debug "sunsetTime: $sunsetTime"
	
	if (state.sunsetTime != sunsetTime.time) {
		state.sunsetTime = sunsetTime.time

		unschedule("sunsetHandler")
		if(sunsetTime.before(now)) {
			sunsetTime.next()
		}
		log.info "scheduling sunset handler for a sunset at: $sunsetTime"
		runDaily(sunsetTime, sunsetHandler)
	}
}

def sunsetHandler() {
	log.info "Executing sunset handler. Will turn off the outlet at 11:59pm."
	if (foyerOutlet) {
		foyerOutlet.on()
	}
	changeMode(sunriseMode)
    outletOff()
}

def changeMode(newMode) {
	if (newMode && location.mode != newMode) {
		if (location.modes?.find{it.name == newMode}) {
			setLocationMode(newMode)
			send "${label} has changed the mode to '${newMode}'"
		}
		else {
			send "${label} tried to change to undefined mode '${newMode}'"
		}
	}
}

private send(msg) {
	if ( sendPushMessage != "No" ) {
		log.debug( "sending push message" )
		sendPush( msg )
	}

	if ( phoneNumber ) {
		log.debug( "sending text message" )
		sendSms( phoneNumber, msg )
	}

	log.debug msg
}

private getLabel() {
	app.label ?: "SmartThings"
}

private getSunsetOffset() {
	sunsetOffsetValue ? (sunsetOffsetDir == "Before" ? "-$sunsetOffsetValue" : sunsetOffsetValue) : null
}

def outletOff() {
    log.debug "called outletsOff() and delaying turning off the outlet till ${time1}"
    //log.debug "called outletsOff() and delaying turning off the outlet at 11:59pm"
    //schedule("00 59 23 * * ?", scheduledTurnOff)
    schedule(time1, scheduledTurnOff)
}

def scheduledTurnOff() {
	foyerOutlet.off()
	unschedule("scheduledTurnOff") // Temporary work-around to scheduling bug
}
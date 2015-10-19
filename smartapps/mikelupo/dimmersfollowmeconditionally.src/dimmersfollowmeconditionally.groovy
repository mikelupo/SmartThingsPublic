/**
 *  LightsFollowMeConditionally
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
 */
definition(
    name: "dimmersFollowMeConditionally",
    namespace: "mikelupo",
    author: "michael lupo",
    description: "Detected motion turns on switch devices (for a duration, in seconds) when the detected light falls below a given level.",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("When motion is detected on ...") {
        input "motionSensor","capability.motionSensor", title: "Select a motion sensor", multiple: true
	}
    section ("...and sample the light value from (Sensor)") {
    	input "lightSensor", "capability.illuminanceMeasurement", title: "Select an illuminance sensor"
        input "lightValue", "number", title: "Lux Value is darker than", required: true
    }
    section("Turn on the following...") {
    	input "light", "capability.switchLevel", title: "Turn on?", required: true, multiple: true
        input "dimmerValue", "number", title: "Set Dimmer Level %", required: true
    }
    section("Turn it off after..."){
        input "seconds", "number", title: "Seconds", required: true
    }
}

def myIlluminanceHandler(evt) {
		def currentIlluminance = "${evt.integerValue}"
        def lastKnownIlluminance = state.lastKnownIlluminance
		log.debug "The current illumanance sent by the event is: ${evt.integerValue}"
        log.debug "The last known illuminance was: ${lastKnownIlluminance}"
        log.debug "Your selected LUX value is: ${lightValue}"
        
        //now update the persisted stored value (state) to hold the new value.
        state.lastKnownIlluminance = evt.integerValue
        def lastStatus = state.lastStatus
}

def myMotionHandler(evt) {
		def motionValue = "${evt.value}"
		log.debug "The motion sent by the event is: ${motionValue}"

        def lastKnownIlluminanceValue = state.lastKnownIlluminance
        log.debug "Last known illuminance is: ${lastKnownIlluminanceValue}"

        def lastStatus = "${state.lastStatus}"
        log.debug "Last status: ${lastStatus}"
 
        if ( "${motionValue}" == "active" && lastKnownIlluminanceValue.intValue() < lightValue  && lastStatus != "on") {
        		log.debug("Turning the light on with a dimmer value of ${dimmerValue}")
                light.setLevel(dimmerValue)
                state.lastStatus = "on"
                state.isTurnedOnByMotion = true

                //turn off the light after motion is stopped and the given delay is expired if the light was turned on by motion.
        } else if ("${motionValue}" == "inactive" && lastStatus == "on" && state.isTurnedOnByMotion == true) {
                //This sets a timer to call the "scheduledTurnOff" method when the light comes on as a result of motion.
                //def delay = seconds * 60   //use this if we decide to use minutes instead of seconds.
                def delay = seconds
                log.debug "Will turn off the light in ${delay} seconds"
                runIn(delay, "scheduledTurnOff")
        } else if ("${motionValue}" == "active" && lastStatus == "on" && state.isTurnedOnByMotion == true) {
        		def delay = seconds
                log.debug "Will turn off the light in another ${delay} seconds"
        }
}

//TODO: create a status.motionDetected map to know whether the light was turned on as a result of motion, or as a result of a manual toggle.
def mySwitchHandler(evt) {
    log.debug "Someone turned the light ${evt.value}"
    
    //this handle the case where we may have turned the light off manually or via the timer.
    if ("${evt.value}" == "off"){
    	state.isTurnedOnByMotion = false
    }
    state.lastStatus = "${evt.value}"
}

def scheduledTurnOff() {
	light.setLevel(0)
}

def installed() {
		//state is a persistently stored map.
		state.lastKnownIlluminance = 0;
        log.debug "Installed with settings: ${settings}"
        subscribe(lightSensor, "illuminance", myIlluminanceHandler)
        subscribe(motionSensor, "motion", myMotionHandler)
        subscribe(light, "switch.on", mySwitchHandler)
        subscribe(light, "switch.off", mySwitchHandler)
        state.isTurnedOnByMotion = false
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
    installed()
}

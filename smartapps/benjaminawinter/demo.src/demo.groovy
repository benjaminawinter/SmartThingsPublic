/**
 *  Demo
 *
 *  Copyright 2016 Ben Winter
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
    name: "Demo",
    namespace: "benjaminawinter",
    author: "Ben Winter",
    description: "Demo SmartApp",
    category: "",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
//Motion detectors
    section("Turn on when motion detected:") {
        input "LivingRoom", "capability.motionSensor", required: true, title: "Where?"
    }
    section("Turn on when motion detected:") {
        input "Kitchen", "capability.motionSensor", required: true, title: "Where?"
    }
    section("Turn on when motion detected:") {
        input "DiningRoom", "capability.motionSensor", required: true, title: "Where?"
    }
    section("Turn on when motion detected:") {
        input "Hall", "capability.motionSensor", required: true, title: "Where?"
    }
    section("Turn on when motion detected:") {
        input "Bedroom", "capability.motionSensor", required: true, title: "Where?"
    }
    
    //Lights
    section("Turn on this light") {
        input "theswitch", "capability.switch", required: true
    }
    
    section("Turn off when there's been no movement for") {
        input "minutes", "number", required: true, title: "Minutes?"
    }
    
}

def installed() {
    initialize()
}

def updated() {
    unsubscribe()
    initialize()
}

def initialize() {
    subscribe(LivingRoom, "motion.active", motionDetectedHandler)
    subscribe(LivingRoom, "motion.inactive", motionStoppedHandler)
    
    subscribe(Kitchen, "motion.active", motionDetectedHandler)
    subscribe(Kitchen, "motion.inactive", motionStoppedHandler)
    
    subscribe(DiningRoom, "motion.active", motionDetectedHandler)
    subscribe(DiningRoom, "motion.inactive", motionStoppedHandler)
    
    subscribe(Hall, "motion.active", motionDetectedHandler)
    subscribe(Hall, "motion.inactive", motionStoppedHandler)
    
    subscribe(Bedroom, "motion.active", motionDetectedHandler)
    subscribe(Bedroom, "motion.inactive", motionStoppedHandler)
}

def motionDetectedHandler(evt) {
    log.debug "motionDetectedHandler called: $evt.device"
    theswitch.on()
    String deviceName = "$evt.device"
    
    def params = [
    	uri: "https://s2hjzofzdf.execute-api.us-east-1.amazonaws.com/dev/motion",
   		body: [
                detectorName: deviceName,
                event: "Motion Detected",
                message: "some logging text",
                timestamp: now()
    		]
		]
        
        try {
        
            httpPostJson(params) { resp ->
                resp.headers.each {
                    log.debug "${it.name} : ${it.value}"
                }
                log.debug "response contentType: ${resp.    contentType}"
            }
        } catch (e) {
            log.debug "something went wrong: $e"
        }
}

def motionStoppedHandler(evt) {
    log.debug "motionStoppedHandler called: $evt.device"
    String deviceName = "$evt.device"
    
    def params = [
    	uri: "https://s2hjzofzdf.execute-api.us-east-1.amazonaws.com/dev/motion",
   		body: [
                detectorName: deviceName,
                event: "Motion Stopped",
                message: "some logging text",
                timestamp: now()
    		]
		]

        try {
            httpPostJson(params) { resp ->
                resp.headers.each {
                    log.debug "${it.name} : ${it.value}"
                }
                log.debug "response contentType: ${resp.    contentType}"
            }
        } catch (e) {
            log.debug "something went wrong: $e"
        }
        
        runIn(60 * minutes, checkMotion(evt))
}

def checkMotion(event) {
    log.debug "In checkMotion scheduled method, device: " + event.device

    def motionState = event.device.currentState("motion")
    
    if (motionState.value == "inactive") {
        // get the time elapsed between now and when the motion reported inactive
        def elapsed = now() - motionState.date.time

        // elapsed time is in milliseconds, so the threshold must be converted to milliseconds too
        def threshold = 1000 * 60 * minutes

        if (elapsed >= threshold) {
            log.debug "Motion has stayed inactive long enough since last check ($elapsed ms):  turning switch off"
            theswitch.off()
        } else {
            log.debug "Motion has not stayed inactive long enough since last check ($elapsed ms):  doing nothing"
        }
    } else {
        // Motion active; just log it and do nothing
        log.debug "Motion is active, do nothing and wait for inactive"
    }
}
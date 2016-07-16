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
        input "LivingRoomMotion", "capability.motionSensor", required: true, title: "Where?"
    }
    section("Turn on when motion detected:") {
        input "KitchenMotion", "capability.motionSensor", required: true, title: "Where?"
    }
    section("Turn on when motion detected:") {
        input "DiningRoomMotion", "capability.motionSensor", required: true, title: "Where?"
    }
    section("Turn on when motion detected:") {
        input "HallMotion", "capability.motionSensor", required: true, title: "Where?"
    }
    section("Turn on when motion detected:") {
        input "BedroomMotion", "capability.motionSensor", required: true, title: "Where?"
    }
    
    //Lights
    section("Turn on this light") {
        input "LivingRoomLight", "capability.switch", required: true
    }
    section("Turn on this light") {
        input "KitchenLight", "capability.switch", required: true
    }
    section("Turn on this light") {
        input "DiningRoomLight", "capability.switch", required: true
    }
    section("Turn on this light") {
        input "HallLight", "capability.switch", required: true
    }
    section("Turn on this light") {
        input "BedroomLight", "capability.switch", required: true
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
    subscribe(LivingRoomMotion, "motion.active", motionDetectedHandler)
    subscribe(LivingRoomMotion, "motion.inactive", motionStoppedHandler)
    
    subscribe(KitchenMotion, "motion.active", motionDetectedHandler)
    subscribe(KitchenMotion, "motion.inactive", motionStoppedHandler)
    
    subscribe(DiningRoomMotion, "motion.active", motionDetectedHandler)
    subscribe(DiningRoomMotion, "motion.inactive", motionStoppedHandler)
    
    subscribe(HallMotion, "motion.active", motionDetectedHandler)
    subscribe(HallMotion, "motion.inactive", motionStoppedHandler)
    
    subscribe(BedroomMotion, "motion.active", motionDetectedHandler)
    subscribe(BedroomMotion, "motion.inactive", motionStoppedHandler)
}

def motionDetectedHandler(evt) {
    log.debug "motionDetectedHandler called: $evt.device"
    String deviceName = "$evt.device"
    
    postMotionEvent(deviceName, "Motion Detected", "Motion Detected Event Detected")
    
    if(deviceName == "LivingRoomMotion"){
    	LivingRoomLight.on()
    }
    if(deviceName == "KitchenMotion"){
    	KitchenLight.on()
    }
    if(deviceName == "DiningRoomMotion"){
    	DiningRoomLight.on()
    }
    if(deviceName == "HallMotion"){
    	HallLight.on()
    }
    if(deviceName == "BedroomMotion"){
    	BedroomLight.on()
    }
}

def motionStoppedHandler(evt) {
    log.debug "motionStoppedHandler called: $evt.device"
    String deviceName = "$evt.device"
    
    postMotionEvent(deviceName, "Motion Stopped", "Motion Stopped Event Detected")
    
    if(deviceName == "LivingRoomMotion"){
    	runIn(60 * minutes, checkMotionLivingRoom)
    }
    if(deviceName == "KitchenMotion"){
    	runIn(60 * minutes, checkMotionKitchen)
    }
    if(deviceName == "DiningRoomMotion"){
    	runIn(60 * minutes, checkMotionDiningRoom)
    }
    if(deviceName == "HallMotion"){
    	runIn(60 * minutes, checkMotionHall)
    }
    if(deviceName == "BedroomMotion"){
    	runIn(60 * minutes, checkMotionBedroom)
    }
    
    
}

def checkMotionLivingRoom() {
    def motionState = LivingRoomMotion.currentState("motion")
    checkMotion(motionState, LivingRoomLight)
}

def checkMotionKitchen() {
    def motionState = KitchenMotion.currentState("motion")
    checkMotion(motionState, KitchenLight)
}

def checkMotionDiningRoom() {
    def motionState = DiningRoomMotion.currentState("motion")
    checkMotion(motionState, DiningRoomLight)
}

def checkMotionHall() {
    def motionState = HallMotion.currentState("motion")
    checkMotion(motionState, HallLight)
}

def checkMotionBedroom() {
    def motionState = BedroomMotion.currentState("motion")
    checkMotion(motionState, BedroomLight)
}

def checkMotion(motionState, theswitch){
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

def postMotionEvent(deviceName, event, message){
	def params = [
    	uri: "https://s2hjzofzdf.execute-api.us-east-1.amazonaws.com/dev/motion",
   		body: [
                detectorName: deviceName,
                event: event,
                message: message,
                timestamp: now()
    		]
		]

        try {
            httpPostJson(params) { resp ->
                resp.headers.each {
                    log.debug "${it.name} : ${it.value}"
                }
                log.debug "response contentType: ${resp.contentType}"
            }
        } catch (e) {
            log.debug "something went wrong: $e"
        }
}
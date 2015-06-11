/**
 *  Smart Light
 *
 *  Copyright 2015 Michael Melancon
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
metadata {
	definition (name: "Smart Light", namespace: "melancon", author: "Michael Melancon") {
		capability "Switch Level"
		capability "Actuator"
		capability "Color Control"
		capability "Switch"
		capability "Sensor"

        command "reset"
        command "sync"
	}

	simulator {
		// TODO: define status and reply messages here
	}

	standardTile("switch", "device.switch", width: 1, height: 1, canChangeIcon: true) {
		state "on", label:'${name}', action:"switch.off", icon:"st.lights.multi-light-bulb-on", backgroundColor:"#79b821", nextState:"turningOff"
		state "off", label:'${name}', action:"switch.on", icon:"st.lights.multi-light-bulb-off", backgroundColor:"#ffffff", nextState:"turningOn"
		state "turningOn", label:'${name}', action:"switch.off", icon:"st.lights.multi-light-bulb-on", backgroundColor:"#79b821", nextState:"turningOff"
		state "turningOff", label:'${name}', action:"switch.on", icon:"st.lights.multi-light-bulb-off", backgroundColor:"#ffffff", nextState:"turningOn"
	}

	standardTile("reset", "device.level", decoration: "flat", inactiveLabel: false) {
		state "default", label:"Reset Color", action:"reset", icon:"st.illuminance.illuminance.dark"
	}

	standardTile("sync", "device.level", decoration: "flat", inactiveLabel: false) {
		state "default", label:"Sync", action:"sync", icon:"st.secondary.refresh-icon"
	}

	controlTile("setLevel", "device.level", "slider", height: 1, width: 2, range:"(0..100)") {
		state "default", action:"switch level.setLevel"
	}

	valueTile("level", "device.level") {
		state "default", label: 'Level ${currentValue}%'
	}

	controlTile("setColor", "device.color", "color", height: 2, width: 2) {
		state "default", action:"setColor"
	}

	valueTile("hue", "device.hue") {
		state "default", label: 'Hue ${currentValue}%'
	}

	valueTile("saturation", "device.saturation") {
		state "default", label: 'Sat ${currentValue}%'
	}

	main(["switch"])

	details(["switch", "setLevel", "setColor", "sync", "reset", "hue", "saturation"])
}

def parse(description) {
	log.debug "parse() - $description"
	def results = []
	def map = description
	if (map?.name && map?.value) {
		results << createEvent(name: "${map?.name}", value: "${map?.value}")
	}
	results
}

def on() {
	log.debug "Executing 'on'"
	sendEvent(name: "switch", value: "on")
}

def off() {
	log.debug "Executing 'off'"
	sendEvent(name: "switch", value: "off")
}

def setLevel(percent) {
	log.debug "Executing 'setLevel($percent)'"
	sendEvent(name: "level", value: percent)
}

def setSaturation(percent) {
	log.debug "Executing 'setSaturation($percent)'"
	sendEvent(name: "saturation", value: percent)
}

def setHue(percent) {
	log.debug "Executing 'setHue($percent)'"
	sendEvent(name: "hue", value: percent)
}

def setColor(color) {
	log.debug "Executing 'setColor($color)'"
    if (color?.hex != null) { sendEvent(name: "color", value: color.hex)}
	if (color?.hue != null) { sendEvent(name: "hue", value: color.hue)}
	if (color?.saturation != null) { sendEvent(name: "saturation", value: color.saturation)}
}

def reset() {
	log.debug "Executing 'reset'"
    def color = [saturation: 0, hue: 0, hex: '#ffffff', red: 255, green: 255, blue: 255, alpha: 1]
    setColor(color)
}

def sync() {
	log.debug "Executing 'sync'"
  	sendEvent(name: "switch", value: device.currentValue("switch"))
    setLevel(device.currentValue("level"))
    setColor([saturation: device.currentValue("saturation"), hue: device.currentValue("hue")])
}

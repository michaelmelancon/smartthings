/**
*  Smart Light - CT
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
	definition (name: "Smart Light - CT", namespace: "melancon", author: "Michael Melancon") {
		capability "Actuator"
		capability "Sensor"
		capability "Switch"
		capability "Switch Level"

		command "sync"
		command "setColorTemperature", ["number"]

		attribute "colorTemperature", "number"
	}

	standardTile("switch", "device.switch", width:2, height: 2, canChangeIcon: true) {
		state "on", label:'${name}', action:"switch.off", icon:"st.lights.multi-light-bulb-on", backgroundColor:"#79b821", nextState:"turningOff"
		state "off", label:'${name}', action:"switch.on", icon:"st.lights.multi-light-bulb-off", backgroundColor:"#ffffff", nextState:"turningOn"
		state "turningOn", label:'${name}', action:"switch.off", icon:"st.lights.multi-light-bulb-on", backgroundColor:"#79b821", nextState:"turningOff"
		state "turningOff", label:'${name}', action:"switch.on", icon:"st.lights.multi-light-bulb-off", backgroundColor:"#ffffff", nextState:"turningOn"
	}

	standardTile("sync", "device.level", decoration: "flat", inactiveLabel: false) {
		state "default", label:"Sync", action:"sync", icon:"st.secondary.refresh-icon"
	}

	controlTile("levelControl", "device.level", "slider", height: 1, width: 3, range:"(0..100)") {
		state "default", action:"switch level.setLevel"
	}

	controlTile("colorTemperatureControl", "device.colorTemperature", "slider", height: 1, width: 3, range:"(153..500)",
	backgroundColors:[
	[value: 150, color: "#bc2323"],
	[value: 250, color: "#d04e00"],
	[value: 300, color: "#f1d801"],
	[value: 325, color: "#44b621"],
	[value: 350, color: "#90d2a7"],
	[value: 400, color: "#1e9cbb"],
	[value: 500, color: "#153591"]
	]) {
		state "default", action:"setColorTemperature"
	}

	main(["switch"])

	details(["switch", "sync", "levelControl", "colorTemperatureControl"])
}

def parse(description) {
}

def on() {
	log.debug "Executing 'on'"
	parent.on(this)
	sendEvent(name: "switch", value: "on")
}

def off() {
	log.debug "Executing 'off'"
	parent.off(this)
	sendEvent(name: "switch", value: "off")
}

def setLevel(percent) {
	log.debug "Executing 'setLevel($percent)'"
	parent.setLevel(this, percent)
	sendEvent(name: "level", value: percent)
}

def setColorTemperature(mirek) {
	log.debug "Executing 'setColorTemperature($mirek)'"
	parent.setColorTemperature(this, mirek)
	sendEvent(name: "colorTemperature", value: mirek)
}

def sync() {
	log.debug "Executing 'sync'"
}

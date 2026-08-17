/**
 * LG TV Fork TRATO — VETRA-Compatible Driver
 *
 * Fork do driver "LGTV with webOS" de Dan Danache (v1.8.3)
 *   Original: https://codeberg.org/dan-danache/hubitat/src/branch/main/lgtv-drivers
 * Developer: ryuleal
 * Company: TecnoSimples
 *
 * O core de comunicação webOS deste driver é IDÊNTICO ao original.
 * Apenas foram adicionadas:
 *   • Capabilities SamsungTV, PushableButton e Variable (para reconhecimento VETRA)
 *   • Comandos com nomes GW8 / SamsungTV que são ALIASES para o código original
 *   • Atributos extras (power, channel, volume, picture, sound, movieMode, etc.)
 *   • Mapeamento push(N) de 58 botões padrão GW8
 */
import groovy.json.JsonBuilder
import groovy.transform.CompileStatic
import groovy.transform.Field

import hubitat.device.HubAction
import hubitat.device.Protocol
import hubitat.helper.NetworkUtils

import com.hubitat.app.ChildDeviceWrapper

@Field static final String DRIVER_NAME = 'LG TV Fork TRATO'
@Field static final String DRIVER_VERSION = '1.8.3-TRATO-1.0'

@Field static final List<String> PICTURE_MODES = ['cinema', 'eco', 'expert1', 'expert2', 'game', 'normal', 'photo', 'sports', 'technicolor', 'vivid', 'hdrEffect', 'filmMaker', 'hdrCinema']
@Field static final List<String> SOUND_MODES = ['aiSoundPlus', 'aiSound', 'standard', 'news', 'music', 'movie', 'sports', 'game']
@Field static final List<String> SOUND_OUTPUT = ['tv_speaker', 'external_arc', 'external_optical', 'bt_soundbar', 'mobile_phone', 'lineout', 'headphone', 'tv_speaker_bluetooth']
@Field static final Integer MAX_FAST_PING = 20

metadata {
    definition(name:DRIVER_NAME, namespace:'TRATO', author:'ryuleal / TecnoSimples (fork de Dan Danache)', importUrl:'https://codeberg.org/dan-danache/hubitat/raw/branch/main/lgtv-drivers/lgtv-with-webos.groovy') {
        // ---- Capabilities originais LG (inalteradas) ----
        capability 'Actuator'
        capability 'Initialize'
        capability 'Refresh'
        capability 'Switch'
        capability 'AudioVolume'
        capability 'TV'
        capability 'Notification'
        capability 'MediaController'
        capability 'MediaTransport'
        capability 'ImageCapture'
        capability 'SpeechSynthesis'

        // ---- Capabilities extras VETRA ----
        capability 'SamsungTV'
        capability 'PushableButton'
        capability 'Variable'

        // ---- Atributos originais LG ----
        attribute 'websocket', 'enum', ['connecting', 'online', 'offline']
        attribute 'channelName', 'string'
        attribute 'screen', 'enum', ['on', 'off', 'standby', 'screensaver']
        attribute 'pictureBrightness', 'number'
        attribute 'pictureContrast', 'number'
        attribute 'pictureColor', 'number'
        attribute 'pictureBacklight', 'number'
        attribute 'pictureMode', 'enum', PICTURE_MODES
        attribute 'soundOutput', 'enum', SOUND_OUTPUT
        attribute 'soundMode', 'enum', SOUND_MODES
        attribute 'pingInterval', 'number'

        // ---- Atributos extras VETRA / GW8 ----
        attribute 'numberOfButtons', 'number'
        attribute 'pushed', 'number'
        attribute 'channel', 'number'
        attribute 'volume', 'number'
        attribute 'power', 'string'
        attribute 'movieMode', 'string'
        attribute 'sound', 'string'
        attribute 'picture', 'string'
        attribute 'Controle', 'string'
        attribute 'TipoControle', 'string'
        attribute 'Formato', 'string'

        // ---- Atributos de diagnóstico ----
        attribute 'lastDisconnectAt', 'string'
        attribute 'disconnectCount', 'number'
        attribute 'reconnectCount', 'number'
        attribute 'lastReconnectAt', 'string'
    }

    // ---- Comandos originais LG (inalterados) ----
    command 'deviceNotification', [
        [name:'Text*', type:'STRING', description:'Notification text*'],
        [name:'Type', type:'ENUM', description:'Notification type', constraints:['Toast - Goes away after few seconds', 'Alert - Stays on screen until dismissed']]
    ]
    command 'setChannel', [[name:'Channel number*', type:'STRING']]
    command 'screenOn'
    command 'screenOff'
    command 'setPicture', [
        [name:'Brightness', type:'NUMBER', description:'Picture brightness (1 to 100)'],
        [name:'Contrast', type:'NUMBER', description:'Picture contrast (1 to 100)'],
        [name:'Color', type:'NUMBER', description:'Picture color (1 to 100)'],
        [name:'Backlight', type:'NUMBER', description:'Picture backlight (1 to 100)'],
        [name:'Mode', type:'ENUM', description:'Select picture mode', constraints:PICTURE_MODES.sort()]
    ]
    command 'setPingInterval', [[name:'Interval*', type:'NUMBER', description:'How often to check if the TV was turned on using the remote (in minutes, 0=disabled)']]
    command 'setSoundOutput', [[name:'Output*', type:'ENUM', description:'Select sound output', constraints:SOUND_OUTPUT.sort()]]
    command 'startVideo', [[name:'URL*', type:'STRING', description:'URL of video file to play']]
    command 'startWebPage', [[name:'URL*', type:'STRING', description:'URL to open in Web Browser']]
    command 'screenSaverOn'
    command 'screenSaverOff'
    command 'pushRemoteButtons', [[name:'TV Remote buttons sequence *', type:'STRING']]

    // ---- Comandos extras VETRA / GW8 (aliases) ----
    command 'poweron'
    command 'poweroff'
    command 'arrowUp'
    command 'arrowDown'
    command 'arrowLeft'
    command 'arrowRight'
    command 'enter'
    command 'confirm'
    command 'back'
    command 'exit'
    command 'home'
    command 'menu'
    command 'source'
    command 'sourceToggle'
    command 'hdmi1'
    command 'hdmi2'
    command 'hdmi'
    command 'channelUp'
    command 'channelDown'
    command 'volumeUp'
    command 'volumeDown'
    command 'mute'
    command 'unmute'
    command 'num0'
    command 'num1'
    command 'num2'
    command 'num3'
    command 'num4'
    command 'num5'
    command 'num6'
    command 'num7'
    command 'num8'
    command 'num9'
    command 'appNetflix'
    command 'appYouTube'
    command 'appAmazonPrime'
    command 'appHBO'
    command 'appDisney'
    command 'appOpenByName', ['string']
    command 'openAppById', ['string']
    command 'info'
    command 'guide'
    command 'smarthub'
    command 'tools'
    command 'previousChannel'
    command 'channelList'
    command 'backIRsend'
    command 'fastBack'
    command 'play'
    command 'pause'
    command 'stop'
    command 'rewind'
    command 'fastForward'
    command 'next'
    command 'btnA'
    command 'btnB'
    command 'btnC'
    command 'btnD'
    command 'btnextra1'
    command 'btnextra2'
    command 'btnextra3'
    command 'btnextra4'
    command 'btnextra5'
    command 'btnextra6'
    command 'btnextra7'
    command 'recreateRemoteChild'
    command 'diagnoseChild'
    command 'listApps'

    preferences {
        input(
            name:'helpInfo', type:'hidden',
            title:"""
            <div style="min-height:55px; background:transparent url('https://codeberg.org/dan-danache/hubitat/raw/branch/main/lgtv-drivers/img/lgtv.webp') no-repeat left center;background-size:auto 55px;padding-left:65px">
                ${DRIVER_NAME} <small>v${DRIVER_VERSION}</small><br>
                <small><div>
                • Fork do <a href="https://codeberg.org/dan-danache/hubitat/src/branch/main/lgtv-drivers" target="_blank">LGTV with webOS</a> por Dan Danache<br>
                • Adaptado para compatibilidade VETRA / GW8<br>
                </div></small>
            </div>
            """
        )
        input(
            name:'logLevel', type:'enum', title:'Log verbosity', required:true,
            description: 'Choose the messages that appear in the "Logs" section',
            options: [
                '1' : 'Debug - log everything',
                '2' : 'Info - log important events',
                '3' : 'Warning - log events that require attention',
                '4' : 'Error - log errors'
            ],
            defaultValue: '1'
        )
        input(
            name:'ipAddr', type:'string', title:'IP address', required:true,
            description: 'Enter the device IP address'
        )
        input(
            name:'useSSL', type:'bool', title:'Enable SSL', required:true,
            description:'Enable SSL when connecting to the TV websocket',
            defaultValue:true
        )
        input(
            name:'simulateRemote', type:'bool', title:'Simulate TV Remote', required:true,
            description:'Allows you to simulate TV Remote button pushes (creates a child device)',
            defaultValue:false
        )
    }
}

// Called when the device is first added
void installed() {
    log_warn 'Installing device ...'

    // Init state
    state.activities = [:]
    state.disconnectCount = 0
    state.reconnectCount = 0
    state.lastDisconnectAt = ''
    state.lastReconnectAt = ''
    state.childDeviceStatus = 'pending'
    utils_sendEvent name:'switch', value:'off', descriptionText:'Power initialized to off', type:'digital'
    utils_sendEvent name:'websocket', value:'offline', descriptionText:'Websocket initialized to offline', type:'digital'
    utils_sendEvent name:'power', value:'off', descriptionText:'Power initialized to off', type:'digital'
    utils_sendEvent name:'Controle', value:'LG webOS', descriptionText:'Controle is LG webOS', type:'digital'
    utils_sendEvent name:'TipoControle', value:'tv', descriptionText:'TipoControle is tv', type:'digital'
    utils_sendEvent name:'Formato', value:'websocket', descriptionText:'Formato is websocket', type:'digital'
    setPingInterval 5
}

// Called when the "Save Preferences" button is clicked
void updated(boolean auto = false) {
    log_info "Saving preferences${auto ? ' (auto)' : ''} ..."

    if (logLevel == null) {
        logLevel = '1'
        device.updateSetting 'logLevel', [value:logLevel, type:'enum']
    }
    if (logLevel == '1') runIn 1800, 'logsOff'
    log_info "🛠️ Log verbosity = ${logLevel}"

    // Update device network Id
    if (ipAddr != null) {
        device.deviceNetworkId = ipAddr.tokenize('.').collect { String.format('%02X', it.toInteger()) }.join()
        connect()
        log_info "🛠️ IP address = ${ipAddr}"
    }

    if (useSSL == null) {
        useSSL = true
        device.updateSetting 'useSSL', [value:useSSL, type:'bool']
    }
    log_info "🛠️ Enable SSL = ${useSSL}"

    if (simulateRemote == null) {
        simulateRemote = false
        device.updateSetting 'simulateRemote', [value:simulateRemote, type:'bool']
    }
    log_info "🛠️ Simulate TV Remote = ${simulateRemote}"
    ensureRemoteChild(simulateRemote)
    // Tentativa adicional com delay para contornar possiveis problemas de timing no Hubitat
    runIn 5, 'delayedEnsureRemoteChild'

    // VETRA init
    utils_sendEvent name:'numberOfButtons', value:58, descriptionText:'Number of buttons is 58', type:'digital'
}

void delayedEnsureRemoteChild() {
    log_info "🛠️ delayedEnsureRemoteChild() running (5s delay) ..."
    ensureRemoteChild(simulateRemote ?: false)
}

private void ensureRemoteChild(boolean shouldExist) {
    String parentDni = device.deviceNetworkId
    if (!parentDni) {
        log_warn "🛠️ Parent DNI is empty. Cannot create child device until IP address is set."
        state.childDeviceStatus = 'parent_dni_empty'
        return
    }
    String childDni = "${parentDni}-Remote"
    log_info "🛠️ Parent DNI = ${parentDni}, Child DNI = ${childDni}"

    if (shouldExist) {
        ChildDeviceWrapper childDevice = getChildDevice(childDni)
        if (childDevice) {
            log_info "🛠️ Child device already exists: ${childDevice}"
            state.childDeviceStatus = 'exists'
            return
        }
        try {
            log_info "🛠️ Creating child device 'LGTV Remote TRATO' with DNI=${childDni} ..."
            childDevice = addChildDevice('TRATO', 'LGTV Remote TRATO', childDni, [name:'LGTV Remote TRATO', label:"${device.displayName} - Remote", isComponent:true])
            log_info "🛠️ addChildDevice returned: ${childDevice}"
            if (childDevice) {
                log_info "🛠️ Child device created successfully (isComponent=true): ${childDevice}"
                state.childDeviceStatus = 'created'
            } else {
                log_warn "🛠️ addChildDevice returned null. Trying without isComponent=true ..."
                childDevice = addChildDevice('TRATO', 'LGTV Remote TRATO', childDni, [name:'LGTV Remote TRATO', label:"${device.displayName} - Remote"])
                log_info "🛠️ addChildDevice (no isComponent) returned: ${childDevice}"
                if (childDevice) {
                    log_info "🛠️ Child device created successfully (isComponent=false): ${childDevice}"
                    state.childDeviceStatus = 'created_no_component'
                } else {
                    log_error "🛠️ addChildDevice returned null both times."
                    state.childDeviceStatus = 'addChildDevice_returned_null'
                }
            }
        } catch (Exception e) {
            log_error "🛠️ FAILED to create child device: ${e.message}"
            log_warn "🛠️ Make sure the driver 'LGTV Remote TRATO' is installed under 'Drivers Code'."
            state.childDeviceStatus = "error: ${e.message}"
        }
    } else {
        try {
            ChildDeviceWrapper childDevice = getChildDevice(childDni)
            if (childDevice) {
                log_debug "🛠️ Removing child device ${childDevice} ..."
                deleteChildDevice(childDni)
                state.childDeviceStatus = 'removed'
            } else {
                state.childDeviceStatus = 'not_present'
            }
        } catch (Exception e) {
            log_error "🛠️ FAILED to remove child device: ${e.message}"
            state.childDeviceStatus = "remove_error: ${e.message}"
        }
    }
}

void setPingInterval(BigDecimal pingInterval) {
    if (pingInterval < 0) {
        log_error("Invalid ping interval specified: ${pingInterval} minutes")
        return
    }

    // Update attribute
    int newValue = pingInterval.toInteger()
    utils_sendEvent name:'pingInterval', value:newValue, unit:'minute', descriptionText:"Ping interval is ${newValue} min ${newValue == 0 ? ' (disabled)' : ''}", type:'digital'

    // Schedule periodic device ping
    unschedule('pingDevice')
    if (newValue == 0) return
    schedule "0 0/${newValue} * ? * * *", 'pingDevice'

    // Ping right now
    pingDevice()
}
void pingDevice() {
    if (!ipAddr || "${device.currentValue('websocket', true)}" == 'online') return
    log_debug "Pinging ${ipAddr} ..."
    if (NetworkUtils.ping(ipAddr, 1)?.packetsReceived > 0) connect()
}
void logsOff() {
    log_info '⏲️ Automatically reverting log level to "Info"'
    device.updateSetting 'logLevel', [value:'2', type:'enum']
}

// ===================================================================================================================
// Implement Capabilities
// ===================================================================================================================

// capability.Initialize
// This method will run when the hub starts
void initialize() {
    state.lastTx = 0
    state.lastRx = 0
    connect(true)
}

// capability.Refresh
void refresh() {
    log_debug '🎬 Refreshing device state ...'

    // We also have subscription for this values
    utils_sendMessage([type:'request', uri:'ssap://audio/getVolume'])
    utils_sendMessage([type:'request', uri:'ssap://tv/getCurrentChannel'])

    // These values rarely change, so we only request them on demand
    utils_sendMessage([type:'request', uri:'ssap://com.webos.service.update/getCurrentSWInformation'])
    utils_sendMessage([type:'request', uri:'ssap://settings/getSystemSettings', payload:[category:'network', keys: ['deviceName']]])
    utils_sendMessage([type:'request', uri:'ssap://config/getConfigs', payload:[configNames:['tv.nyx.*']]])
    utils_sendMessage([type:'request', uri:'ssap://com.webos.service.connectionmanager/getinfo'])

    // https://github.com/JPersson77/LGTVCompanion/blob/master/Docs/Commandline.md
    // https://github.com/chros73/bscpylgtv/blob/master/docs/available_settings_CX.md
    //utils_sendMessage([type:'request', uri:'ssap://settings/getSystemSettings', payload:[category:'sound', keys: ['soundMode']]])
    //utils_sendMessage([type:'request', uri:'ssap://config/getConfigs', payload:[configNames:['audio.*']]])
}

// capability.Switch
void on() {
    log_debug '🎬 Powering on ...'
    util_wakeOnLan getDataValue('wifiMacAddress')
    util_wakeOnLan getDataValue('wiredMacAddress')
    if (ipAddr) util_wakeOnLan getMACFromIP(ipAddr)

    // Start fast pinging the IP for a maximum of MAX_FAST_PING times
    runIn 1, 'fastPing', [data:[currentRetry:0]]
}
private void fastPing(Map data) {
    data.currentRetry += 1
    if (!ipAddr || "${device.currentValue('switch', true)}" == 'on' || data.currentRetry > MAX_FAST_PING) {
        log_debug 'Fast ping terminated'
        return
    }

    log_debug "Fast pinging ${ipAddr}: ${data.currentRetry} / ${MAX_FAST_PING} ..."
    if (NetworkUtils.ping(ipAddr, 1)?.packetsReceived > 0 && "${device.currentValue('switch', true)}" != 'on') connect()

    // Keep fast-pinging until switch changes to 'on' or MAX_FAST_PING limit is reached
    runIn 1, 'fastPing', [data:data]
}
void off() {
    log_debug '🎬 Powering off ...'
    utils_sendMessage([type:'request', uri:'ssap://system/turnOff'])
    utils_sendEvent name:'switch', value:'off', descriptionText:'Power is off', type:'digital'
}

// capability.AudioVolume
void mute() {
    log_debug '🎬 Muting sound ...'
    utils_sendMessage([type:'request', uri:'ssap://audio/setMute', payload:[mute:true]])
}
void unmute() {
    log_debug '🎬 Unmuting sound ...'
    utils_sendMessage([type:'request', uri:'ssap://audio/setMute', payload:[mute:false]])
}
void volumeUp() {
    log_debug '🎬 Raising volume ...'
    utils_sendMessage([type:'request', uri:'ssap://audio/volumeUp'])
}
void volumeDown() {
    log_debug '🎬 Lowering volume ...'
    utils_sendMessage([type:'request', uri:'ssap://audio/volumeDown'])
}
void setVolume(String level) { setVolume Integer.parseInt(level) }
void setVolume(BigDecimal level) {
    log_debug "🎬 Setting volume to ${level}% ..."
    if (level < 0 || level > 100) return
    utils_sendMessage([type:'request', uri:'ssap://audio/setVolume', payload:[volume:level]])
}

// capability.TV
void channelUp() {
    log_debug '🎬 Changing channel up ...'
    utils_sendMessage([type:'request', uri:'ssap://tv/channelUp'])
}
void channelDown() {
    log_debug '🎬 Changing channel down ...'
    utils_sendMessage([type:'request', uri:'ssap://tv/channelDown'])
}

// capability.Notification
void deviceNotification(String text, String type = 'Toast') {
    log_debug "🎬 Sending ${type} notification ..."
    if (type.startsWith('Alert') || text.startsWith('!')) {
        utils_sendMessage([type:'request', uri:'"ssap://system.notifications/createAlert', payload:[message:text.replaceAll('^!', ''), buttons:[[label:'Dismiss']]]])
    } else {
        utils_sendMessage([type:'request', uri:'"ssap://system.notifications/createToast', payload:[message:text]])
    }
}

// capability.MediaController
void getAllActivities() {
    log_debug '🎬 Getting all activities ...'

    // Remove old activities
    Map<String, String> activities = [:]
    activities['com.webos.app.home'] = 'Home'
    activities['com.webos.app.livetv'] = 'Live TV'
    activities['com.webos.app.miracast'] = 'Miracast'
    state.activities = activities

    utils_sendMessage([type:'request', uri:'ssap://tv/getExternalInputList'])
    utils_sendMessage([type:'request', uri:'ssap://com.webos.applicationManager/listLaunchPoints'])
}
void getCurrentActivity() {
    log_debug '🎬 Getting current activity ...'
    utils_sendMessage([type:'request', uri:'ssap://com.webos.applicationManager/getForegroundAppInfo'])
}
void startActivity(String activityname) {
    String appId = state.activities.find { it.value == activityname }?.key ?: activityname
    log_debug "🎬 Starting activity: [${activityname}] (${appId}) ..."
    utils_sendMessage([type:'request', uri:'ssap://system.launcher/launch', payload:[id:appId]])
}

// capability.MediaTransport
void play() {
    log_debug '🎬 Play ...'
    utils_sendMessage([type:'request', uri:'ssap://media.controls/play'])
    utils_sendEvent name:'transportStatus', value:'playing', descriptionText:'Media transport status is playing', type:'digital'
}
void pause() {
    log_debug '🎬 Pause ...'
    utils_sendMessage([type:'request', uri:'ssap://media.controls/pause'])
    utils_sendEvent name:'transportStatus', value:'paused', descriptionText:'Media transport status is paused', type:'digital'
}
void stop() {
    log_debug '🎬 Stop ...'
    utils_sendMessage([type:'request', uri:'ssap://media.controls/stop'])
    utils_sendEvent name:'transportStatus', value:'stopped', descriptionText:'Media transport status is stopped', type:'digital'
}

// capability.ImageCapture
void take() {
    log_debug '🎬 Taking a screenshot ...'
    utils_sendMessage([type:'request', uri:'ssap://tv/executeOneShot', payload:[path:'/tmp/capture.jpg', method:'DISPLAY', format:'JPG']])
}

// capability.SpeechSynthesis
void speak(String text, BigDecimal volume = null, String voice = null) {
    log_debug "🎬 Speaking text [${text}] ..."
    Map result = textToSpeech(text, voice)
    log_debug "Sending TTS file ${result} ..."
    // utils_sendMessage([type:'request', uri:'ssap://com.webos.applicationManager/open', payload:[target:result.uri, mime:'audio/mp3']])
    utils_sendMessage([type:'request', uri:'ssap://com.webos.applicationManager/launch', payload:[
        id: state.activities.find { it.value == 'Media Player' || it.value == 'Music' }?.key ?: 'com.webos.app.mediadiscovery',
        params: [payload: [[
            fullPath:result.uri, mediaType:'MUSIC', deviceType:'DMR', lastPlayPosition:0,
            fileName: "Message from ${location.hub.name}",
            thumbnail: "http://${location.hub.localIP}/ui2/images/apple-touch-icon.png",
            dlnaInfo: [
                protocolInfo: 'http-get:*:audio/mpeg:DLNA.ORG_PN=MP3;DLNA.ORG_OP=01;DLNA.ORG_FLAGS=01500000000000000000000000000000',
                contentLength: '-1',
                duration: result.duration,
                opVal: 1,
                flagVal: 0,
                cleartextSize: '-1'
            ]
        ]]]
    ]])
}

// ===================================================================================================================
// Implement custom commands
// ===================================================================================================================
void setChannel(String channel) {
    log_debug "🎬 Changing channel to [${channel}] ..."
    utils_sendMessage([type:'request', uri:'ssap://tv/openChannel', payload:[channelNumber:channel]])
}
void screenOn() {
    log_debug '🎬 Turning screen on ...'
    utils_sendMessage([type:'request', uri:'ssap://com.webos.service.tvpower/power/turnOnScreen', payload:[standbyMode:'active']])
}
void screenOff() {
    log_debug '🎬 Turning screen off ...'
    utils_sendMessage([type:'request', uri:'ssap://com.webos.service.tvpower/power/turnOffScreen', payload:[standbyMode:'active']])
}
void setPicture(BigDecimal brightness, BigDecimal contrast = null, BigDecimal color = null, BigDecimal backlight = null, String mode = null) {
    log_debug "🎬 Setting picture to: brightness=${brightness}, contrast=${contrast}, color=${color}, backlight=${backlight}, mode=${mode}] ..."
    Map<String, String> settings = [:]
    if (brightness && brightness >= 1 && brightness <= 100) settings.put 'brightness', brightness.intValue().toString()
    if (contrast && contrast >= 1 && contrast <= 100) settings.put 'contrast', contrast.intValue().toString()
    if (color && color >= 1 && color <= 100) settings.put 'color', color.intValue().toString()
    if (backlight && backlight >= 1 && backlight <= 100) settings.put 'backlight', backlight.intValue().toString()
    if (mode) settings.put 'pictureMode', mode
    if (settings.size() > 0) utils_sendMessage([type:'request', uri:'ssap://settings/setSystemSettings', payload:[category:'picture', settings:settings]])
}
void setSoundOutput(String output) {
    log_debug "🎬 Setting sound output to: [${output}] ..."
    utils_sendMessage([type:'request', uri:'ssap://settings/setSystemSettings', payload:[category:'sound', settings: [soundOutput:output]]])
    //utils_sendMessage([type:'request', uri:'com.webos.service.apiadapter/audio/changeSoundOutput', payload:[output:output]]])
}
void startVideo(String url) {
    log_debug "🎬 Playing video file: [${url}]..."

    // https://gist.github.com/aabytt/bddbb1bcf031a050d89a89aeee3a6737#playling-a-link-with-standard-lg-webos-player
    utils_sendMessage([type:'request', uri:'ssap://com.webos.applicationManager/launch', payload:[
        id: state.activities.find { it.value == 'Media Player' || it.value == 'Photo & Video' }?.key ?: 'com.webos.app.mediadiscovery',
        params: [payload: [[fullPath:url, mediaType:'VIDEO', deviceType:'DMR', lastPlayPosition:0]]]
    ]])
}
void startWebPage(String url) {
    log_debug "🎬 Opening web page: [${url}]..."
    utils_sendMessage([type:'request', uri:'ssap://com.webos.applicationManager/launch', payload:[
        id: 'com.webos.app.browser',
        params: [target:url]
    ]])
}
void screenSaverOn() {
    log_debug '🎬 Starting screen saver ...'
    utils_sendMessage([type:'request', uri:'ssap://system.launcher/launch', payload:[id:'com.webos.app.screensaver']])
}
void screenSaverOff() {
    log_debug '🎬 Stopping screen saver ...'
    utils_sendMessage([type:'request', uri:'ssap://system.launcher/close', payload:[id:'com.webos.app.screensaver']])
}
void pushRemoteButtons(String buttonSequence) {
    if (!simulateRemote) {
        log_info 'The "Simulate TV Remote" option is currently disabled in the "Preferences" tab'
        state.remove 'remoteButtonSequence'
        return
    }
    if ("${device.currentValue('switch', true)}" != 'on') {
        log_info 'Cannot push TV Remote buttons as device is not switched on'
        state.remove 'remoteButtonSequence'
        return
    }

    log_debug "🎬 Pushing TV Remote buttons: ${buttonSequence} ..."
    state.remoteButtonSequence = buttonSequence.toUpperCase()
    utils_sendMessage([type:'request', uri:'ssap://com.webos.service.networkinput/getPointerInputSocket'])
}

// ===================================================================================================================
// Websocket helpers
// ===================================================================================================================

void connect(boolean force = false) {
    if (!ipAddr) {
        log_warn 'Device IP address not set in Preferences tab. Make sure you are using DHCP reservation for the TV\'s IP address!'
        return
    }

    // A connection is already in progress
    if (!force && "${device.currentValue('websocket', true)}" == 'connecting') {

        // Don't start a new connection if websocket status is in 'connecting' state for less than 25 seconds
        int timeoutIn = state.containsKey('lastWx') ? state.lastWx + 25000 - now() : 0
        if (timeoutIn > 0) {
            log_debug "✋ Another connection is already in progress; it will time out in ${timeoutIn/1000} seconds"
            return
        }
    }

    log_info "▲ Connecting to ${ipAddr} ${force ? '(forced) ' : ''}..."
    disconnect()
    interfaces.webSocket.connect(useSSL ? "wss://${ipAddr}:3001/" : "ws://${ipAddr}:3000/", headers: ['Content-Type': 'application/json'], ignoreSSLIssues: true)

    // Update "websocket" attribute
    utils_sendEvent name:'websocket', value:'connecting', descriptionText:'Websocket is connecting', type:'digital'
    state.lastWx = now()

    // Diagnostic counters
    int rc = (state.reconnectCount ?: 0) + 1
    state.reconnectCount = rc
    String ts = new Date().format("yyyy-MM-dd HH:mm:ss")
    utils_sendEvent name:'reconnectCount', value:rc, descriptionText:"Reconnect count is ${rc}", type:'digital'
    utils_sendEvent name:'lastReconnectAt', value:ts, descriptionText:"Last reconnect at ${ts}", type:'digital'
}
void disconnect() {
    try { interfaces.webSocket.close() } catch (e) { }
}
void register() {
    if (state.pk) {
        utils_sendMessage([type:'register', payload:['client-key':state.pk]], false)
        return
    }

    utils_sendMessage([
        type: 'register',
        payload: [
            pairingType: 'PROMPT',
            manifest: [
                appVersion: '1.1',
                manifestVersion: 1,
                permissions: ['LAUNCH', 'LAUNCH_WEBAPP', 'APP_TO_APP', 'CLOSE', 'TEST_OPEN', 'TEST_PROTECTED', 'CONTROL_AUDIO', 'CONTROL_DISPLAY', 'CONTROL_INPUT_JOYSTICK', 'CONTROL_INPUT_MEDIA_RECORDING', 'CONTROL_INPUT_MEDIA_PLAYBACK', 'CONTROL_INPUT_TV', 'CONTROL_POWER', 'READ_APP_STATUS', 'READ_CURRENT_CHANNEL', 'READ_INPUT_DEVICE_LIST', 'READ_NETWORK_STATE', 'READ_RUNNING_APPS', 'READ_TV_CHANNEL_LIST', 'WRITE_NOTIFICATION_TOAST', 'READ_POWER_STATE', 'READ_COUNTRY_INFO', 'READ_SETTINGS', 'CONTROL_TV_SCREEN', 'CONTROL_TV_STANBY', 'CONTROL_FAVORITE_GROUP', 'CONTROL_USER_INFO', 'CHECK_BLUETOOTH_DEVICE', 'CONTROL_BLUETOOTH', 'CONTROL_TIMER_INFO', 'STB_INTERNAL_CONNECTION', 'CONTROL_RECORDING', 'READ_RECORDING_STATE', 'WRITE_RECORDING_LIST', 'READ_RECORDING_LIST', 'READ_RECORDING_SCHEDULE', 'WRITE_RECORDING_SCHEDULE', 'READ_STORAGE_DEVICE_LIST', 'READ_TV_PROGRAM_INFO', 'CONTROL_BOX_CHANNEL', 'READ_TV_ACR_AUTH_TOKEN', 'READ_TV_CONTENT_STATE', 'READ_TV_CURRENT_TIME', 'ADD_LAUNCHER_CHANNEL', 'SET_CHANNEL_SKIP', 'RELEASE_CHANNEL_SKIP', 'CONTROL_CHANNEL_BLOCK', 'DELETE_SELECT_CHANNEL', 'CONTROL_CHANNEL_GROUP', 'SCAN_TV_CHANNELS', 'CONTROL_TV_POWER', 'CONTROL_WOL'],
                signatures: [[
                    signatureVersion: 1,
                    signature: 'eyJhbGdvcml0aG0iOiJSU0EtU0hBMjU2Iiwia2V5SWQiOiJ0ZXN0LXNpZ25pbmctY2VydCIsInNpZ25hdHVyZVZlcnNpb24iOjF9.hrVRgjCwXVvE2OOSpDZ58hR+59aFNwYDyjQgKk3auukd7pcegmE2CzPCa0bJ0ZsRAcKkCTJrWo5iDzNhMBWRyaMOv5zWSrthlf7G128qvIlpMT0YNY+n/FaOHE73uLrS/g7swl3/qH/BGFG2Hu4RlL48eb3lLKqTt2xKHdCs6Cd4RMfJPYnzgvI4BNrFUKsjkcu+WD4OO2A27Pq1n50cMchmcaXadJhGrOqH5YmHdOCj5NSHzJYrsW0HPlpuAx/ECMeIZYDh6RMqaFM2DXzdKX9NmmyqzJ3o/0lkk/N97gfVRLW5hA29yeAwaCViZNCP8iC9aO0q9fQojoa7NQnAtw==',
                ]],
                signed: [
                    created: '20140509',
                    appId: 'com.lge.test',
                    vendorId: 'com.lge',
                    localizedAppNames: ['':'LG Remote App', 'ko-KR':'리모컨 앱', 'zxx-XX':'ЛГ Rэмotэ AПП'],
                    localizedVendorNames: ['': 'LG Electronics'],
                    permissions: ['TEST_SECURE', 'CONTROL_INPUT_TEXT', 'CONTROL_MOUSE_AND_KEYBOARD', 'READ_INSTALLED_APPS', 'READ_LGE_SDX', 'READ_NOTIFICATIONS', 'SEARCH', 'WRITE_SETTINGS', 'WRITE_NOTIFICATION_ALERT', 'CONTROL_POWER', 'READ_CURRENT_CHANNEL', 'READ_RUNNING_APPS', 'READ_UPDATE_INFO', 'UPDATE_FROM_REMOTE_APP', 'READ_LGE_TV_INPUT_EVENTS', 'READ_TV_CURRENT_TIME'],
                    serial: '2f930e2d2cfe083771f68e4fe7bb07'
                ]
            ]
        ]
    ], false)
}
void subscribe(Map data = false) {

    // Setup subscriptions
    utils_sendMessage([type:'subscribe', uri:'ssap://audio/getStatus'])
    utils_sendMessage([type:'subscribe', uri:'ssap://tv/getCurrentChannel'])
    utils_sendMessage([type:'subscribe', uri:'ssap://com.webos.applicationManager/getForegroundAppInfo'])
    utils_sendMessage([type:'subscribe', uri:'ssap://com.webos.media/getForegroundAppInfo'])
    utils_sendMessage([type:'subscribe', uri:'ssap://com.webos.service.tvpower/power/getPowerState'])
    utils_sendMessage([type:'subscribe', uri:'ssap://settings/getSystemSettings', payload:[category:'picture', keys:['brightness', 'contrast', 'color', 'backlight', 'pictureMode']]])
    utils_sendMessage([type:'subscribe', uri:'ssap://settings/getSystemSettings', payload:[category:'sound', keys:['soundMode', 'soundOutput']]])

    if (data.firstTime) {

        // Enable Wake on LAN
        log_debug '🎬 Enabling Wake On LAN (WOL) ...'
        utils_sendMessage([type:'request', uri:'ssap://settings/setSystemSettings', payload:[category:'network', settings: [wolwowlOnOff:'true']]], false)

        // Send a notification on TV
        deviceNotification "<b>Mesage from ${location.hub.name}</b><br>Well done! Configuration is now complete 👍"
    }
}

// ===================================================================================================================
// Websocket callback
// ===================================================================================================================

void webSocketStatus(String message) {
    log_debug "▶ webSocketStatus(${message})"

    String websocket = utils_parseStatus message
    String prev = device.currentValue('websocket', true) ?: 'unknown'

    // Track disconnections for diagnostics
    if (websocket == 'offline' && prev == 'online') {
        int dc = (state.disconnectCount ?: 0) + 1
        state.disconnectCount = dc
        String ts = new Date().format("yyyy-MM-dd HH:mm:ss")
        log_warn "▶ Websocket DISCONNECTED (count=${dc}) at ${ts}. Message: ${message}"
        utils_sendEvent name:'disconnectCount', value:dc, descriptionText:"Disconnect count is ${dc}", type:'physical'
        utils_sendEvent name:'lastDisconnectAt', value:ts, descriptionText:"Last disconnect at ${ts}", type:'physical'
    }

    // If websocket just opened, say hello (skip state checks)
    if (websocket == 'online') {
        if (prev == 'offline' || prev == 'connecting') {
            log_info "▶ Websocket CONNECTED after disconnect."
        }
        utils_sendMessage([type:'hello', id:'hello'], false)
        return
    }

    // Update "websocket" attribute
    utils_sendEvent name:'websocket', value:websocket, descriptionText:"Websocket is ${websocket}", type:'physical'
}

void parse(String description) {
    log_debug "▶ Received message: ${description}"
    state.lastRx = now()

    Map msg = parseJson(description)
    String type = msg.payload?.callerId == 'secondscreen.client' || msg.payload?.callerId == 'com.webos.service.apiadapter' || now() - (state.lastTx ?: 0) < 2000 ? 'digital' : 'physical'

    // Empty reponse. Thanks for nothing!
    if (msg.payload.keySet().size() == 1 && (msg.payload.returnValue == true || msg.payload.subscription == true)) return

    switch (msg.type) {

        // TV sent an error message. Oh noes!
     case 'error':

            // Ignore unsupported config keys and TV bind fails
            if (msg.payload?.errorText?.startsWith('Some keys are not allowed') || msg.payload?.errorText?.startsWith('com.webos.service.utp/bind returns invalid result')) return

            // Live TV weird error
            if (msg.payload?.errorText ==~ /^broadcastId = .* is already closed$/) return

            // Service not supported on some (old) devices
            if (msg.error == '404 no such service or method') return

            log_error "▶ Received error message: ${msg.payload?.errorText ? msg.payload?.errorText : msg.error}"
        return

        // Hello (request)
        case 'hello':
            log_debug '▶ Proper protocol has been observed. Starting authentication ...'

            // Ask for device information (required on WebOS 33.20.66+)
            // @see https://github.com/home-assistant-libs/aiowebostv/pull/501
            utils_sendMessage([type:'request', uri:'ssap://system/getSystemInfo'], false)
            return

        // Register (request)
        case 'registered':
            log_debug '▶ Authentication complete. Oh yeah!'

            // Update "websocket" and "switch" attributes
            utils_sendEvent name:'websocket', value:'online', descriptionText:'Websocket is online', type:'physical'
            utils_sendEvent name:'switch', value:'on', descriptionText:'Power is on', type:'physical'

            // Setup subscriptions
            runIn 1, 'subscribe', [data:[firstTime:!state.containsKey('pk')]]
            state.pk = msg.payload['client-key']

            // Auto-sync device state with Hubitat
            runIn 7, 'refresh'
            return

        // TV sent a response to a command sent by us
        case 'response':
            Map payload = msg.payload
            switch (payload) {

                // ssap://audio/getStatus (subscription)
                case { contains it, [volumeStatus:null] }:
                    int volume = payload.volumeStatus.volume
                    String mute = payload.volumeStatus.muteStatus ? 'muted' : 'unmuted'
                    String soundOutput = payload.volumeStatus.soundOutput ?: ''
                    utils_sendEvent name:'volume', value:volume, unit:'%', descriptionText:"Sound volume is ${volume}%", type:type
                    utils_sendEvent name:'mute', value:mute, descriptionText:"Sound is ${mute}", type:type
                    utils_sendEvent name:'soundOutput', value:soundOutput, descriptionText:"Sound output is ${soundOutput}", type:type
                    return

                case { contains it, [mute:null, volume:null] }:
                    int volume = payload.volume
                    String mute = payload.mute ? 'muted' : 'unmuted'
                    utils_sendEvent name:'volume', value:volume, unit:'%', descriptionText:"Sound volume is ${volume}%", type:type
                    utils_sendEvent name:'mute', value:mute, descriptionText:"Sound is ${mute}", type:type
                    return

                // ssap://tv/getCurrentChannel (subscription)
                case { contains it, [channelNumber:null] }:
                    String channel = payload.channelNumber
                    String channelName = payload.channelName ?: 'unknown'
                    utils_sendEvent name:'channel', value:channel, descriptionText:"Channel is ${channel}", type:type
                    utils_sendEvent name:'channelName', value:channelName, descriptionText:"Channel name is ${channelName}", type:type
                    return

                // ssap://com.webos.applicationManager/getForegroundAppInfo (subscription)
                case { contains it, [appId:'', processId:'', windowId:''] }:
                    utils_sendEvent name:'switch', value:'off', descriptionText:'Power is off', type:type
                    utils_sendEvent name:'currentActivity', value:'off', descriptionText:"Current activity is off", type:type
                    disconnect()
                    return

                case { contains it, [appId:null] }:
                    utils_sendEvent name:'switch', value:'on', descriptionText:'Power is on', type:type
                    log_info "🎬 FOREGROUND APP CHANGED: appId=${payload.appId}"
                    String currentActivity = state.activities?.find { it.key == payload.appId }?.value ?: 'unknown'
                    utils_sendEvent name:'currentActivity', value:currentActivity, descriptionText:"Current activity is ${currentActivity}", type:type
                    return

                // ssap://settings/getSystemSettings (subscription)
                case { contains it, [category:'picture'] }:
                    String pictureBrightness = payload.settings?.brightness ?: ''
                    String pictureContrast = payload.settings?.contrast ?: ''
                    String pictureColor = payload.settings?.color ?: ''
                    String pictureBacklight = payload.settings?.backlight ?: ''
                    String pictureMode = payload.settings?.pictureMode ?: ''
                    utils_sendEvent name:'pictureBrightness', value:pictureBrightness, unit:'%', descriptionText:"Picture brightness is ${pictureBrightness}%", type:type
                    utils_sendEvent name:'pictureContrast', value:pictureContrast, unit:'%', descriptionText:"Picture contrast is ${pictureContrast}%", type:type
                    utils_sendEvent name:'pictureColor', value:pictureColor, unit:'%', descriptionText:"Picture color is ${pictureColor}%", type:type
                    utils_sendEvent name:'pictureBacklight', value:pictureBacklight, unit:'%', descriptionText:"Picture backlight is ${pictureBacklight}%", type:type
                    utils_sendEvent name:'pictureMode', value:pictureMode, descriptionText:"Picture mode is ${pictureMode}", type:type
                    return

                case { contains it, [category:'sound'] }:
                    String soundOutput = payload.settings?.soundOutput
                    utils_sendEvent name:'soundOutput', value:soundOutput, descriptionText:"Sound output is ${soundOutput}", type:type

                    String soundMode = payload.settings?.soundMode
                    utils_sendEvent name:'soundMode', value:soundMode, descriptionText:"Sound mode is ${soundMode}", type:type
                    return

                // ssap://com.webos.media/getForegroundAppInfo (subscription)
                case { contains it, [foregroundAppInfo:null] }:
                    String transportStatus = utils_parseForegroundAppInfo(payload.foregroundAppInfo)
                    utils_sendEvent name:'transportStatus', value:transportStatus, descriptionText:"Media transport status is ${transportStatus}", type:type
                    return

                // ssap://tv/getExternalInputList (request)
                case { contains it, [devices:null] }:
                    Map activities = state.activities ?: [:]
                    payload.devices.each { activities[it.appId] = it.label }
                    state.activities = activities
                    runIn 5, 'updateActivities'
                    return

                // ssap://com.webos.applicationManager/listLaunchPoints (request)
                case { contains it, [launchPoints:null] }:
                    Map activities = state.activities ?: [:]
                    payload.launchPoints.each { activities[it.id] = it.title }
                    state.activities = activities
                    log_info "🎬 listLaunchPoints response: ${payload.launchPoints.collect { "${it.id}=${it.title}" }.join(', ')}"
                    runIn 5, 'updateActivities'
                    return

                // ssap://com.webos.applicationManager/listApps (request)
                case { contains it, [apps:null] }:
                    log_info "🎬 listApps response: ${payload.apps.collect { "${it.id}=${it.title}" }.join(', ')}"
                    return

                // ssap://settings/getSystemSettings (request)
                case { contains it, [category:'network'] }:
                    utils_dataValue 'networkName', payload.settings?.deviceName
                    return

                // ssap://com.webos.service.update/getCurrentSWInformation (request)
                case { contains it, [sw_type:null] }:
                    utils_dataValue 'fwVersion', "${payload.product_name} / ${payload.major_ver}.${payload.minor_ver}"
                    return

                // ssap://com.webos.service.tvpower/power/turnOnScreen (request)
                case { contains it, [state:'Screen Off'] }:
                    utils_sendEvent name:'screen', value:'off', descriptionText:'Screen is off', type:type
                    return

                case { contains it, [state:'Active'] }:
                    utils_sendEvent name:'screen', value:'on', descriptionText:'Screen is on', type:type
                    return

                case { contains it, [state:'Screen Saver'] }:
                    utils_sendEvent name:'screen', value:'screensaver', descriptionText:'Screen is in screensaver mode', type:type
                    return

                case { contains it, [state:'Active Standby'] }:
                    utils_sendEvent name:'screen', value:'standby', descriptionText:'Screen is in active standby', type:type
                    return

                // ssap://system/getSystemInfo (request)
                case { contains it, [modelName:null] }:
                    utils_dataValue 'modelName', payload.modelName
                    utils_dataValue 'receiverType', payload.receiverType

                    // Auto-start registration
                    runIn 1, 'register'
                    return

                // ssap://com.webos.service.connectionmanager/getinfo (request)
                case { contains it, [wifiInfo:null] }:
                case { contains it, [wiredInfo:null] }:
                    utils_dataValue 'wifiMacAddress', payload.wifiInfo?.macAddress
                    utils_dataValue 'wiredMacAddress', payload.wiredInfo?.macAddress
                    return

                // ssap://config/getConfigs (request)
                case { contains it, [configs:null] }:
                    utils_dataValue 'platformVersion', payload.configs['tv.nyx.platformVersion']
                    return

                // ssap://com.webos.service.networkinput/getPointerInputSocket (request)
                case { contains it, [socketPath:null] }:
                    getChildDevice("${device.deviceNetworkId}-Remote").pushButtons(payload.socketPath, "${state.remoteButtonSequence}")
                    state.remove 'remoteButtonSequence'
                    return

                // ssap://tv/executeOneShot
                case { contains it, [imageUri:null] }:
                    String image = payload.imageUri
                    utils_sendEvent name:'image', value:image, descriptionText:"Capture URL is ${image}", type:type
                    return

                // Pairing prompt (request)
                case { contains it, [pairingType:'PROMPT'] }:
                    log_warn '🙋‍♂️ Go and accept the pairing request on your TV screen. HAUL ASS!'
                    return

                // ssap://audio/getVolume (request)
                case { contains it, [soundOutput:null] }:
                    String soundOutput = payload.soundOutput ?: ''
                    utils_sendEvent name:'soundOutput', value:soundOutput, descriptionText:"Sound output is ${soundOutput}", type:type

                    if (payload.containsKey('muteStatus')) {
                        String mute = payload.muteStatus ? 'muted' : 'unmuted'
                        utils_sendEvent name:'mute', value:mute, descriptionText:"Sound is ${mute}", type:type
                    }
                    return

                // Other messages we don't care about
                case { contains it, [muteStatus:null] }:
                case { contains it, [sessionId:null] }:
                case { contains it, [volume:null] }:
                case { contains it, [toastId:null] }:
                case { contains it, [alertId:null] }:
                case { contains it, [missingConfigs:null] }:
                case { contains it, [returnValue:true, method:'setSystemSettings'] }:
                    return
            }
    }

    // Unexpected websocket message
    log_error "🚩 Received unexpected websocket message: description=${description}"
}

// ===================================================================================================================
// Logging helpers (something like this should be part of the SDK and not implemented by each driver)
// ===================================================================================================================

private void log_debug(String message, String prefix = device.displayName) {
    if (logLevel == '1') log.debug "${prefix} ${message.uncapitalize()}"
}
private void log_info(String message, String prefix = device.displayName) {
    if (logLevel <= '2') log.info "${prefix} ${message.uncapitalize()}"
}
private void log_warn(String message, String prefix = device.displayName) {
    if (logLevel <= '3') log.warn "${prefix} ${message.uncapitalize()}"
}
private void log_error(String message, String prefix = device.displayName) {
    log.error "${prefix} ${message.uncapitalize()}"
}

// ===================================================================================================================
// Helper methods (keep them simple, keep them dumb)
// ===================================================================================================================

private void utils_sendMessage(Map message, boolean checkState = true) {
    if (!ipAddr) {
        log_warn 'Device IP address not set in Preferences tab. Make sure you are using DHCP reservation for the TV\'s IP Address!'
        return
    }

    if (checkState && "${device.currentValue('switch', true)}" != 'on') {
        log_info 'Device is not switched on. Command not sent.'
        return
    }

    if (checkState && "${device.currentValue('websocket', true)}" != 'online') {
        log_info 'Websocket is not connected anymore. Connecting now ...'
        runIn 1, 'connect'
        return
    }

    if (!message.containsKey('payload')) message.payload = [:]

    // Send websocket message
    state.lastTx = now()
    message.id = "hubitat_${now()}"
    message.sourceId = 'hubitat.hub'

    log_debug "◀ Sending websocket messages: ${message}"
    String payload = new JsonBuilder(message)
    interfaces.webSocket.sendMessage(payload)
}
private void utils_sendEvent(Map event) {
    if (event.value == null || event.value == '') return
    if ("${device.currentValue(event.name, true)}" != "${event.value}") {
        log_info "${event.descriptionText} [${event.type}]"
    } else {
        log_debug "${event.descriptionText} [${event.type}]"
    }
    sendEvent event
}
private void utils_dataValue(String key, String value) {
    if (value == null || value == '') return
    log_debug "Update data value: ${key}=${value}"
    updateDataValue key, value
}
private String utils_parseStatus(String message) {
    switch (message) {
        case 'status: open': return 'online'
        case 'status: closing': return 'offline'
        case ~/^failure: connect timed out.*/: return 'offline'
        case ~/^failure: unexpected end of stream on http:\/\/.*/:
            log_warn 'Failed to establish a stable websocket connection to your TV. You should enable SSL in the Preferences tab. DO IT!'
            return 'offline'
        case ~/^failure: .*/:
            log_info "Oh snap! ${message}"
            return 'offline'
        default: return 'offline'
    }
}
private String utils_parseForegroundAppInfo(List<Map> foregroundAppInfo) {
    String playState = foregroundAppInfo.find { it.windowId != '' }?.playState
    if (playState == 'loaded' || playState == 'playing') return 'playing'
    return playState == 'paused' ? 'paused' : 'stopped'
}
private void util_wakeOnLan(String macAddr) {
    if (macAddr == null || macAddr == '') return
    String cmd = "wake on lan ${macAddr.replaceAll(':', '').toUpperCase()}"
    log_debug "◀ Sending LAN command: ${cmd}"
    sendHubCommand(new HubAction(cmd, Protocol.LAN))
}
void updateActivities() {
    List<String> activities = state.activities*.value.sort()
    utils_sendEvent name:'activities', value:new JsonBuilder(activities), descriptionText:"Supported activities is ${activities}", type:type
}

// switch/case syntactic sugar
@CompileStatic private boolean contains(Map msg, Map spec) {
    return msg.keySet().containsAll(spec.keySet()) && spec.every { it.value == null || it.value == msg[it.key] }
}

// ===================================================================================================================
// VETRA / GW8 Compatibility Layer — ALIASES ONLY
// ===================================================================================================================

@Field static final Map PUSH_BUTTON_MAP = [
    1:"poweron",        2:"mute",           3:"source",         4:"back",
    5:"menu",           6:"hdmi1",          7:"hdmi2",          8:"arrowLeft",
    9:"arrowRight",    10:"arrowUp",       11:"arrowDown",     12:"enter",
    13:"exit",         14:"home",          18:"channelUp",     19:"channelDown",
    21:"volumeUp",     22:"volumeDown",    23:"num0",          24:"num1",
    25:"num2",         26:"num3",          27:"num4",          28:"num5",
    29:"num6",         30:"num7",          31:"num8",          32:"num9",
    33:"btnextra1",    34:"btnextra2",     35:"btnextra3",     36:"appAmazonPrime",
    37:"appPrimeVideo",38:"appAmazonPrime",39:"appYouTube",   40:"appNetflix",
    41:"btnextra4",     42:"btnextra5",
    43:"btnextra6",    44:"btnextra7",     45:"btnA",          46:"btnB",
    47:"btnC",         48:"btnD",          49:"play",          50:"pause",
    51:"next",         52:"guide",         53:"info",          54:"tools",
    55:"smarthub",     56:"previousChannel", 57:"backIRsend", 58:"poweroff"
]

// capability.PushableButton
void push(BigDecimal button) {
    int n = button.toInteger()
    log_debug "🎬 PushableButton push(${n})"
    utils_sendEvent name:'pushed', value:n, descriptionText:"Button ${n} was pushed", type:'digital'
    String cmd = PUSH_BUTTON_MAP[n]
    if (cmd) {
        log_info "push(${n}) -> mapped to '${cmd}'"
        try { this."${cmd}"() } catch (MissingMethodException e) { log_warn "Command '${cmd}' not found for button ${n}" }
    } else {
        log_warn "push(${n}): button not mapped in PUSH_BUTTON_MAP"
    }
}

// capability.Variable (no-op placeholder)
void setVariable(String name, String value) {
    log_debug "Variable setVariable(${name}, ${value}) — no-op"
}

// capability.SamsungTV
void showMessage(String a=null, String b=null, String c=null, String d=null) {
    String msg = [a,b,c,d].findAll { it }.join(' ')
    log_debug "🎬 SamsungTV showMessage: ${msg}"
    deviceNotification(msg, 'Toast')
}
void setPictureMode(String mode) {
    log_debug "🎬 SamsungTV setPictureMode(${mode}) ..."
    setPicture(null, null, null, null, vetrareverseSamsungPictureMode(mode))
}
void setSoundMode(String mode) {
    log_debug "🎬 SamsungTV setSoundMode(${mode}) ..."
    String lgMode = vetrareverseSamsungSoundMode(mode)
    if (lgMode) {
        utils_sendMessage([type:'request', uri:'ssap://settings/setSystemSettings', payload:[category:'sound', settings: [soundMode:lgMode]]])
    }
}
void setInputSource(String sourceName) {
    log_debug "🎬 SamsungTV setInputSource(${sourceName}) ..."
    String normalized = sourceName?.toString()?.toLowerCase()?.trim()
    switch (normalized) {
        case 'hdmi1':     startActivity('com.webos.app.hdmi1'); break
        case 'hdmi2':     startActivity('com.webos.app.hdmi2'); break
        case 'livetv':
        case 'tv':        startActivity('com.webos.app.livetv'); break
        default:          log_warn "setInputSource: '${sourceName}' not mapped"
    }
}

// ---- Power aliases ----
void poweron()  { on() }
void poweroff() { off() }

// ---- Navigation (delegam ao pushRemoteButtons do original, que requer simulateRemote) ----
void arrowUp()    { pushRemoteButtons('UP')    }
void arrowDown()  { pushRemoteButtons('DOWN')  }
void arrowLeft()  { pushRemoteButtons('LEFT')  }
void arrowRight() { pushRemoteButtons('RIGHT') }
void enter()      { pushRemoteButtons('ENTER') }
void confirm()    { enter() }
void back()       { pushRemoteButtons('BACK') }
void exit()       { pushRemoteButtons('EXIT') }
void home()       { pushRemoteButtons('HOME') }
void menu()       { pushRemoteButtons('MENU') }

// ---- Source / HDMI (delegam ao startActivity do original) ----
void source()       { pushRemoteButtons('INPUT_HUB') }
void sourceToggle() { source() }
void hdmi1()        { startActivity('com.webos.app.hdmi1') }
void hdmi2()        { startActivity('com.webos.app.hdmi2') }
void hdmi()         { source() }

// ---- Numeric pad (delegam ao pushRemoteButtons) ----
void num0() { pushRemoteButtons('0') }
void num1() { pushRemoteButtons('1') }
void num2() { pushRemoteButtons('2') }
void num3() { pushRemoteButtons('3') }
void num4() { pushRemoteButtons('4') }
void num5() { pushRemoteButtons('5') }
void num6() { pushRemoteButtons('6') }
void num7() { pushRemoteButtons('7') }
void num8() { pushRemoteButtons('8') }
void num9() { pushRemoteButtons('9') }

// ---- Apps (delegam ao startActivity original) ----
void appNetflix()     { startActivity('netflix') }
void appAmazonPrime() {
    log_info '🎬 appAmazonPrime() called — launching amazon'
    startActivity('amazon')
}
void appPrimeVideo()  { appAmazonPrime() }
void amazonPrime()    { appAmazonPrime() }
void primeVideo()     { appAmazonPrime() }
void prime()          { appAmazonPrime() }
void amazon()         { appAmazonPrime() }
void amazonVideo()    { appAmazonPrime() }
void appYouTube()     { startActivity('youtube.leanback.v4') }
void appOpenByName(String appnamevalue) {
    log_debug "🎬 appOpenByName(${appnamevalue})"
    String n = appnamevalue?.toString()?.trim()
    switch (n) {
        case 'Netflix':      appNetflix(); break
        case 'YouTube':      appYouTube(); break
        case 'Amazon Prime':
        case 'Prime Video':  appAmazonPrime(); break
        case 'HBO':
        case 'HBO Max':      appHBO(); break
        case 'Disney':
        case 'Disney+':      appDisney(); break
        default:             startActivity(n)
    }
}

// ---- Apps: HBO & Disney+ (IDs variam por regiao; tenta os mais comuns) ----
void appHBO() {
    log_debug '🎬 appHBO (HBO Max) ...'
    // ID verificado na TV do usuario: com.wbd.stream (Warner Bros. Discovery Stream)
    // Outros IDs comuns em outras regioes sao mantidos como fallback.
    try { startActivity('com.wbd.stream'); return } catch (Exception e) { }
    try { startActivity('com.wbd.max'); return } catch (Exception e) { }
    try { startActivity('com.hbo.hbomax-prod'); return } catch (Exception e) { }
    try { startActivity('com.hbo.hbomax'); return } catch (Exception e) { }
    try { startActivity('hbomax'); return } catch (Exception e) { }
    try { startActivity('hbo.max'); return } catch (Exception e) { }
    try { startActivity('com.wbd.hbomax'); return } catch (Exception e) { }
    log_warn '🎬 appHBO: none of the common HBO Max app IDs worked. Use openAppById() with your TV\'s exact ID.'
}
void appDisney() {
    log_debug '🎬 appDisney (Disney+) ...'
    // Os IDs abaixo sao os mais comuns em TVs LG webOS.
    // O ID varia por regiao. Na Europa e comum 'com.disney.disneyplus-prod'.
    // Se nao funcionar, use openAppById() com o ID exato da sua TV.
    try { startActivity('com.disney.disneyplus-prod'); return } catch (Exception e) { }
    try { startActivity('com.disney.disneyplus'); return } catch (Exception e) { }
    try { startActivity('disneyplus'); return } catch (Exception e) { }
    try { startActivity('disney+'); return } catch (Exception e) { }
    log_warn '🎬 appDisney: none of the common Disney+ app IDs worked. Use openAppById() with your TV\'s exact ID.'
}
void openAppById(String appId) {
    log_debug "🎬 openAppById(${appId})"
    if (!appId) { log_warn 'openAppById: appId is empty'; return }
    startActivity(appId.trim())
}

// ---- Diagnostic helpers ----
void printAllApps() {
    // Imprime todos os apps conhecidos em chunks pequenos para nao truncar no log do Hubitat
    def apps = state.activities ?: [:]
    if (!apps) {
        log_warn '📋 printAllApps: nenhum app em cache. Execute getAllActivities primeiro.'
        return
    }
    log_info "📋 Total de apps em cache: ${apps.size()}"
    apps.eachWithIndex { id, title, idx ->
        log_info "📋 [${idx + 1}/${apps.size()}] id=${id} | title=${title}"
    }
}

void searchApp(String searchTerm) {
    // Busca apps pelo titulo ou ID contendo o termo
    if (!searchTerm) { log_warn 'searchApp: termo de busca vazio'; return }
    def apps = state.activities ?: [:]
    def termLower = searchTerm.toLowerCase()
    def matches = apps.findAll { id, title ->
        id.toLowerCase().contains(termLower) || title.toLowerCase().contains(termLower)
    }
    if (!matches) {
        log_warn "🔍 searchApp: nenhum resultado para '${searchTerm}'. Execute getAllActivities primeiro se ainda nao executou."
        return
    }
    log_info "🔍 searchApp '${searchTerm}' encontrou ${matches.size()} resultado(s):"
    matches.each { id, title ->
        log_info "🔍   id=${id} | title=${title}"
    }
}

// ---- Special functions ----
void info()            { pushRemoteButtons('INFO') }
void guide()           { pushRemoteButtons('GUIDE') }
void smarthub()        { pushRemoteButtons('HOME') }
void tools()           { pushRemoteButtons('QMENU') }
void previousChannel() { pushRemoteButtons('BACK') }
void channelList()     { pushRemoteButtons('LIST') }
void backIRsend()      { back() }
void fastBack()        { back() }

// ---- Media (delegam ao original quando existe, senão pushRemoteButtons) ----
void rewind()      { pushRemoteButtons('REWIND') }
void fastForward() { pushRemoteButtons('FASTFORWARD') }
void next()        { fastForward() }

// ---- Color buttons ----
void btnA() { pushRemoteButtons('RED')    }
void btnB() { pushRemoteButtons('GREEN')  }
void btnC() { pushRemoteButtons('YELLOW') }
void btnD() { pushRemoteButtons('BLUE')   }

// ---- Extra buttons ----
void btnextra1() { pushRemoteButtons('AD')             }
void btnextra2() { pushRemoteButtons('ASPECT_RATIO')   }
void btnextra3() { pushRemoteButtons('CC')             }
void btnextra4() { pushRemoteButtons('DASH')           }
void btnextra5() { pushRemoteButtons('LIVE_ZOOM')      }
void btnextra6() { pushRemoteButtons('MAGNIFIER_ZOOM') }
void btnextra7() { pushRemoteButtons('TELETEXT')      }

// ---- List installed apps (diagnostic to discover app IDs) ----
void listApps() {
    log_info '🎬 listApps() - requesting installed apps from TV ...'
    utils_sendMessage([type:'request', uri:'ssap://com.webos.applicationManager/listApps'], false)
}

// ---- Manual child recreate ----
void recreateRemoteChild() {
    log_info "🛠️ Manual recreateRemoteChild() called."
    ensureRemoteChild(true)
}

// ---- Child device diagnostics ----
void diagnoseChild() {
    String parentDni = device.deviceNetworkId
    String childDni = parentDni ? "${parentDni}-Remote" : 'N/A'
    log_info "🛠️ DIAGNOSE CHILD:"
    log_info "🛠️   Parent DNI = ${parentDni ?: 'EMPTY'}"
    log_info "🛠️   Child DNI = ${childDni}"
    log_info "🛠️   childDeviceStatus state = ${state.childDeviceStatus ?: 'not set'}"
    log_info "🛠️   simulateRemote setting = ${simulateRemote}"
    if (childDni != 'N/A') {
        try {
            ChildDeviceWrapper childDevice = getChildDevice(childDni)
            log_info "🛠️   getChildDevice result = ${childDevice ?: 'null (not found)'}"
        } catch (Exception e) {
            log_error "🛠️   getChildDevice threw: ${e.message}"
        }
    }
    log_info "🛠️   Available child devices: ${childDevices*.deviceNetworkId}"
}

// ===================================================================================================================
// VETRA helpers
// ===================================================================================================================

private String vetrareverseSamsungPictureMode(String mode) {
    switch (mode?.toLowerCase()) {
        case 'standard': return 'normal'
        case 'movie':    return 'cinema'
        case 'dynamic':  return 'vivid'
        default:         return mode ?: 'normal'
    }
}

private String vetrareverseSamsungSoundMode(String mode) {
    switch (mode?.toLowerCase()) {
        case 'standard': return 'standard'
        case 'movie':    return 'movie'
        case 'music':    return 'music'
        case 'speech':   return 'news'
        default:         return null
    }
}

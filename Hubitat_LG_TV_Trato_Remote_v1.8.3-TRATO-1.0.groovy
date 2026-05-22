/**
 * LGTV Remote TRATO
 *
 * Component driver para o "LG TV Fork TRATO".
 * Fork do driver "LGTV Remote" de Dan Danache (v1.8.3).
 *   Original: https://codeberg.org/dan-danache/hubitat/src/branch/main/lgtv-drivers
 *
 * Este child device é criado pelo driver pai quando "Simulate TV Remote" está
 * habilitado. Reutiliza o websocket de input remoto da TV LG webOS para enviar
 * pressionamentos de botões individuais ou sequências.
 */
import groovy.transform.Field

@Field static final String DRIVER_NAME = 'LGTV Remote TRATO'
@Field static final String DRIVER_VERSION = '1.8.3-TRATO-1.0'

@Field static final List<String> BUTTONS = [
    'LEFT',
    'RIGHT',
    'UP',
    'DOWN',
    'RED',
    'GREEN',
    'YELLOW',
    'BLUE',
    'CHANNELUP',
    'CHANNELDOWN',
    'VOLUMEUP',
    'VOLUMEDOWN',
    'PLAY',
    'PAUSE',
    'STOP',
    'REWIND',
    'FASTFORWARD',
    'ASTERISK',
    'BACK',
    'EXIT',
    'ENTER',
    'AMAZON',
    'NETFLIX',
    '3D_MODE',
    'AD',
    'ASPECT_RATIO',
    'CC',
    'DASH',
    'GUIDE',
    'HOME',
    'INFO',
    'INPUT_HUB',
    'LIST',
    'LIVE_ZOOM',
    'MAGNIFIER_ZOOM',
    'MENU',
    'MUTE',
    'MYAPPS',
    'POWER',
    'PROGRAM',
    'QMENU',
    'RECENT',
    'RECORD',
    'SAP',
    'SCREEN_REMOTE',
    'TELETEXT',
    'TEXTOPTION',
    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
]

metadata {
    definition(name:DRIVER_NAME, component:true, namespace:'TRATO', author:'TRATO (fork de Dan Danache)') {
        attribute 'status', 'enum', ['playing', 'stopped']
    }
    command 'clearQueue'
}

// Called when the device is first added
void installed() {
    log_warn 'Installing device ...'

    // Init state
    state.data = 'This device is used by the LG TV Fork TRATO driver and it\'s pretty useless on its own.'
    state.info = 'Move along, nothing to see here!'
    state.queue = []
    utils_sendEvent name:'status', value:'stopped', descriptionText:'Status is stopped', type:'digital'
}

void clearQueue() {
    unschedule()
    state.queue = []
    utils_sendEvent name:'status', value:'stopped', descriptionText:'Status is stopped', type:'digital'
    try { interfaces.webSocket.close() } catch (e) { }
}

void pushButtons(String websocketURL, String buttonSequence) {
    if (!buttonSequence) return

    // Add to queue
    List queue = state.queue ?: []
    queue.addAll buttonSequence.split(' ')
    state.queue = queue

    // Websocket already active; reuse connection
    if ("${device.currentValue('status', true)}" == 'playing') return

    // Start websocket connection
    try { interfaces.webSocket.close() } catch (e) { }
    interfaces.webSocket.connect websocketURL, ignoreSSLIssues:true
}

void webSocketStatus(String message) {

    // Connection opened
    if (message == 'status: open') {
        utils_sendEvent name:'status', value:'playing', descriptionText:'Status is playing', type:'digital'
        runInMillis 100, 'processNextQueueItem'
        return
    }

    // Websocket closed
    if (message == 'status: closing') return

    // Not good
    log_warn "▶ webSocketStatus: ${message}"
    utils_sendEvent name:'status', value:'stopped', descriptionText:'Status is stopped', type:'digital'
}

void processNextQueueItem() {
    List queue = state.queue ?: []
    if (!queue?.size()) {
        clearQueue()
        return
    }

    Integer wait = 350
    String item = queue.remove(0)

    // Wait item
    if (item.startsWith('WAIT:')) {
        state.queue = queue
        wait = Integer.parseInt item.substring(5)
        runInMillis wait, 'processNextQueueItem'
        return
    }

    // Valid button?
    if (!BUTTONS.contains(item)) {
        log_warn "Invalid TV Remote button: ${item}. Ignored."
        runInMillis wait, 'processNextQueueItem'
        return
    }

    // Send item
    log_debug "◀ Sending ${item}"
    interfaces.webSocket.sendMessage "type:button\nname:${item}\n\n"
    runInMillis wait, 'processNextQueueItem'
}

void parse(String description) {
    log_debug "▶ Received message: ${description}"
}

// ===================================================================================================================
// Logging helpers (something like this should be part of the SDK and not implemented by each driver)
// ===================================================================================================================

private void log_debug(String message) { parent.log_debug message, device.displayName }
private void log_info(String message) { parent.log_info message, device.displayName }
private void log_warn(String message) { parent.log_warn message, device.displayName }

private void utils_sendEvent(Map event) {
    if (event.value == null) return
    if ("${device.currentValue(event.name, true)}" != "${event.value}") {
        log_info "${event.descriptionText} [${event.type}]"
    } else {
        log_debug "${event.descriptionText} [${event.type}]"
    }
    sendEvent event
}

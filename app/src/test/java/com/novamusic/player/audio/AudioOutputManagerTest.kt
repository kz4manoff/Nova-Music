package com.novamusic.player.audio

import android.bluetooth.BluetoothClass
import com.novamusic.player.data.model.AudioOutputDevice
import com.novamusic.player.data.model.AudioOutputType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioOutputManagerTest {

    @Test
    fun `classifyBluetoothDeviceType separates speakers from headphones cleanly`() {
        assertEquals(
            AudioOutputType.BLUETOOTH_SPEAKER,
            classifyBluetoothDeviceType(BluetoothClass.Device.AUDIO_VIDEO_LOUDSPEAKER, "My Audio")
        )
        assertEquals(
            AudioOutputType.BLUETOOTH_HEADPHONES,
            classifyBluetoothDeviceType(BluetoothClass.Device.AUDIO_VIDEO_WEARABLE_HEADSET, "AirPods Pro")
        )
        assertEquals(
            AudioOutputType.BLUETOOTH_SPEAKER,
            classifyBluetoothDeviceType(null, "JBL Flip 6")
        )
        assertEquals(
            AudioOutputType.BLUETOOTH_HEADPHONES,
            classifyBluetoothDeviceType(null, "Sony WH-1000XM5")
        )
    }

    @Test
    fun `resolvePreferredSystemRouteActiveId honors manual user override while connected`() {
        val speaker = AudioOutputDevice(
            id = "sink_1",
            audioDeviceId = 1,
            name = "Phone Speaker",
            type = AudioOutputType.PHONE_SPEAKER
        )
        val airpods = AudioOutputDevice(
            id = "sink_9",
            audioDeviceId = 9,
            name = "AirPods Pro",
            type = AudioOutputType.BLUETOOTH_HEADPHONES
        )

        // Manual switch to Phone Speaker stays active even while Bluetooth is connected
        assertEquals(
            "sink_1",
            resolvePreferredSystemRouteActiveId(
                devices = listOf(speaker, airpods),
                userPreferredDeviceId = "sink_1",
                systemRouteTypeHint = AudioOutputType.BLUETOOTH_HEADPHONES
            )
        )

        // When user reverts to auto (null), Bluetooth headphones take priority or match system hint
        assertEquals(
            "sink_9",
            resolvePreferredSystemRouteActiveId(
                devices = listOf(speaker, airpods),
                userPreferredDeviceId = null,
                systemRouteTypeHint = null
            )
        )

        // If selected Bluetooth device disconnects, falls back to Phone Speaker
        assertEquals(
            "sink_1",
            resolvePreferredSystemRouteActiveId(
                devices = listOf(speaker),
                userPreferredDeviceId = "sink_9",
                systemRouteTypeHint = null
            )
        )
    }

    @Test
    fun `shouldEmitDeviceBanner deduplicates rapid repeated events for same device`() {
        assertTrue(
            shouldEmitDeviceBanner(
                eventKey = "BLUETOOTH_HEADPHONES_aa:bb",
                isConnected = true,
                lastKey = null,
                lastIsConnected = null,
                lastTimestampMs = 0L,
                nowMs = 10_000L
            )
        )
        assertFalse(
            shouldEmitDeviceBanner(
                eventKey = "BLUETOOTH_HEADPHONES_aa:bb",
                isConnected = true,
                lastKey = "BLUETOOTH_HEADPHONES_aa:bb",
                lastIsConnected = true,
                lastTimestampMs = 10_000L,
                nowMs = 10_500L
            )
        )
        assertTrue(
            shouldEmitDeviceBanner(
                eventKey = "BLUETOOTH_HEADPHONES_aa:bb",
                isConnected = false,
                lastKey = "BLUETOOTH_HEADPHONES_aa:bb",
                lastIsConnected = true,
                lastTimestampMs = 10_000L,
                nowMs = 10_500L
            )
        )
    }
}

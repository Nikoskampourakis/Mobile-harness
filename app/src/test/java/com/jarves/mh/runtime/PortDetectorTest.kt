package com.jarves.mh.runtime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PortDetectorTest {

    @Test
    fun resolveServiceNameRecognizesWellKnownPorts() {
        assertEquals("VS Code Server (:8080)", PortDetector.resolveServiceName(8080))
        assertEquals("Vite Dev Server (:5173)", PortDetector.resolveServiceName(5173))
        assertEquals("Web App / Next.js (:3000)", PortDetector.resolveServiceName(3000))
        assertEquals("Flask / Express (:5000)", PortDetector.resolveServiceName(5000))
        assertEquals("Python HTTP / FastAPI (:8000)", PortDetector.resolveServiceName(8000))
        assertEquals("Streamlit App (:8501)", PortDetector.resolveServiceName(8501))
        assertEquals("Web App (:9999)", PortDetector.resolveServiceName(9999))
    }

    @Test
    fun detectedServiceIdentifiesVsCode() {
        val vsCodeService = DetectedService(8080, "VS Code Server (:8080)", "http://localhost:8080", isVsCode = true)
        val webService = DetectedService(3000, "Web App (:3000)", "http://localhost:3000", isVsCode = false)

        assertTrue(vsCodeService.isVsCode)
        assertFalse(webService.isVsCode)
        assertEquals("http://localhost:8080", vsCodeService.url)
    }
}

package com.offlinecurrencyconverter.app

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before

abstract class MockApiServer {
    protected lateinit var mockServer: MockWebServer
    protected lateinit var baseUrl: String

    @Before
    fun setupMockServer() {
        mockServer = MockWebServer()
        mockServer.start()
        baseUrl = mockServer.url("/").toString()
    }

    @After
    fun tearDownMockServer() {
        mockServer.shutdown()
    }

    protected fun enqueueSuccess(body: String) {
        mockServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(body)
                .addHeader("Content-Type", "application/json")
        )
    }

    protected fun enqueueError(statusCode: Int, message: String = "Server Error") {
        mockServer.enqueue(
            MockResponse()
                .setResponseCode(statusCode)
                .setBody("""{"error": "$message"}""")
        )
    }

    protected fun enqueueMalformed() {
        mockServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("not valid json{{{")
        )
    }

    protected fun enqueueEmpty() {
        mockServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("")
        )
    }
}

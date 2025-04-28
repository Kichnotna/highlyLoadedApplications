package org.example.last_project

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Duration
import java.time.LocalTime
import java.util.concurrent.Executors
import kotlin.random.Random

@RestController
class CounterController
constructor(
    private val redisTemplate: StringRedisTemplate,
) {
    @Value("\${server.port}")
    private val serverPort = 0
    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        mapOf(
            10.0 to "Игрок неудачник",
            20.0 to "Игрок нормис",
            30.0 to "Игрок машина XXL",
        ).forEach { (score, name) ->
            redisTemplate.opsForZSet().add(LEADERBOARD, name, score)
        }
    }


    @GetMapping("/")
    fun counter(): ResponseEntity<String> {
        val value = (redisTemplate.opsForValue().get(COUNTER) ?: 0).toString().toInt().inc()
        redisTemplate.opsForValue().set(COUNTER, value.toString())
        val response = "Server running on port $serverPort: $value"

        return ResponseEntity.ok(response)
    }

    @GetMapping("/leaderboard")
    fun leaderboard(): ResponseEntity<Map<String, Double>> {
        val players = redisTemplate.opsForZSet()
            .reverseRangeWithScores(LEADERBOARD, 0, redisTemplate.opsForZSet().size(LEADERBOARD)!! - 1)
            ?.associate { it.value.toString() to it.score!!.toDouble() }

        return ResponseEntity.ok(players)
    }

    @GetMapping("/start")
    fun start(): ResponseEntity<String> {
        Executors.newSingleThreadExecutor().execute {
            runBlocking {
                upload()
            }
        }
        this.startProcessingLoop()

        return ResponseEntity.ok("Started upload")
    }

    private suspend fun upload() {
        for (value in 1..100) {
            redisTemplate.opsForList().leftPush(QUEUE, value.toString())
            val delayMs = Random.nextLong(FROM_DELAY, UNTIL_DELAY)
            delay(delayMs)
        }
    }

    private fun startProcessingLoop() {
        scope.launch {
            while (isActive) {
                redisTemplate.opsForList().rightPop(QUEUE, Duration.ofSeconds(10))?.let {
                    processMessage(it)
                }
            }
        }
    }

    private suspend fun processMessage(message: String) {
        println("[${LocalTime.now()}] Processing: $message")
        delay(FROM_DELAY)
        println("[${LocalTime.now()}] Done: $message")
    }

    companion object {
        private const val COUNTER = "counter"
        private const val LEADERBOARD = "leaderboard"
        private const val QUEUE = "queue"

        private const val FROM_DELAY = 1000L
        private const val UNTIL_DELAY = 5000L
    }
}
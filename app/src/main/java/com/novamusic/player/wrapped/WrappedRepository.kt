package com.novamusic.player.wrapped

import com.novamusic.player.database.dao.ListeningDao
import com.novamusic.player.database.entities.ListeningEventEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class WrappedRepository(
    private val listeningDao: ListeningDao
) {
    suspend fun persistListeningSession(event: ListeningEventEntity) {
        withContext(Dispatchers.IO) {
            listeningDao.upsertSessionEvent(event)
        }
    }

    fun observeWrappedSummary(period: WrappedPeriod): Flow<WrappedSummaryStats> {
        val range = WrappedAnalyticsEngine.buildPeriodRange(period)
        return listeningDao.observeEventsBetween(range.startMs, range.endMs)
            .map { events ->
                WrappedAnalyticsEngine.aggregate(events = events, range = range)
            }
            .flowOn(Dispatchers.Default)
    }

    suspend fun getWrappedSummary(period: WrappedPeriod): WrappedSummaryStats {
        val range = WrappedAnalyticsEngine.buildPeriodRange(period)
        val events = withContext(Dispatchers.IO) {
            listeningDao.getEventsBetween(range.startMs, range.endMs)
        }
        return withContext(Dispatchers.Default) {
            WrappedAnalyticsEngine.aggregate(events = events, range = range)
        }
    }

    suspend fun getTotalEventCount(): Int = withContext(Dispatchers.IO) {
        listeningDao.getTotalEventCount()
    }

    suspend fun clearAllListeningStats() {
        withContext(Dispatchers.IO) {
            listeningDao.clearAllListeningEvents()
        }
    }
}

package com.github.shirleh.persistence.influx

import com.influxdb.client.WriteApi
import com.influxdb.client.write.Point
import io.mockk.mockk
import io.mockk.verify
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.gherkin.Feature

private data class KittyPoint(val numberOfMeows: Int = 69) : InfluxPoint {
    override fun toDataPoint() = Point.measurement("kitty")
}

private class KittyPointRepositoryImpl(writeApi: WriteApi) : InfluxWriteRepositoryImpl<KittyPoint>(writeApi)

object InfluxWriteRepositoryImplTest : Spek({
    Feature("InfluxRepository") {
        val writeApi = mockk<WriteApi>(relaxUnitFun = true)
        val influxRepository by memoized {
            KittyPointRepositoryImpl(writeApi)
        }

        Scenario("saving a single point of data") {
            val kittyData = KittyPoint()

            When("saving a single point of data") {
                influxRepository.save(kittyData)
            }

            Then("it should have written the data to Influx") {
                verify { writeApi.writePoint(any()) }
            }
        }

        Scenario("saving multiple points of data") {
            val kittyData = listOf(KittyPoint(), KittyPoint())

            When("saving multiple points of data") {
                influxRepository.save(kittyData)
            }

            Then("it should have written the data to Influx") {
                verify { writeApi.writePoints(any()) }
            }
        }
    }
})

package com.github.shirleh.statistics.privacy

import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.gherkin.Feature
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class PrivacySettingsFeature : Spek({
    Feature("Privacy settings") {
        val subject = PrivacySettings(userId = 420, guildId = 69)
        val allMeasurements = Measurement.values().asList()

        Scenario("initial state") {
            Then("it should return false for all metrics") {
                assertFalse { subject.message }
                assertFalse { subject.emoji }
                assertFalse { subject.voice }
                assertFalse { subject.nickname }
                assertFalse { subject.membership }
            }
        }

        Scenario("opting in to message metrics") {
            lateinit var result: PrivacySettings

            When("opting in to message metrics") {
                result = subject.optIn(listOf(Measurement.MESSAGE))
            }

            Then("it should return true for message") {
                assertTrue { result.message }
            }

            And("false for all other metrics") {
                assertFalse { result.emoji }
                assertFalse { result.voice }
                assertFalse { result.nickname }
                assertFalse { result.membership }
            }
        }

        Scenario("opting out of message metrics") {
            lateinit var givenSubject: PrivacySettings
            lateinit var result: PrivacySettings

            Given("privacy settings with complete opt-ins") {
                givenSubject = subject.optIn(allMeasurements)
            }

            When("opting out of message metrics") {
                result = givenSubject.optOut(listOf(Measurement.MESSAGE))
            }

            Then("it should return false for message") {
                assertFalse { result.message }
            }

            And("true for all other metrics") {
                assertTrue { result.emoji }
                assertTrue { result.voice }
                assertTrue { result.nickname }
                assertTrue { result.membership }
            }
        }

        Scenario("opting in to emoji metrics") {
            lateinit var result: PrivacySettings

            When("opting in to emoji metrics") {
                result = subject.optIn(listOf(Measurement.EMOJI))
            }

            Then("it should return true for emoji") {
                assertTrue { result.emoji }
            }

            And("false for all other metrics") {
                assertFalse { result.message }
                assertFalse { result.voice }
                assertFalse { result.nickname }
                assertFalse { result.membership }
            }
        }

        Scenario("opting out of emoji metrics") {
            lateinit var givenSubject: PrivacySettings
            lateinit var result: PrivacySettings

            Given("privacy settings with complete opt-ins") {
                givenSubject = subject.optIn(allMeasurements)
            }

            When("opting out of emoji metrics") {
                result = givenSubject.optOut(listOf(Measurement.EMOJI))
            }

            Then("it should return false for emoji") {
                assertFalse { result.emoji }
            }

            And("true for all other metrics") {
                assertTrue { result.message }
                assertTrue { result.voice }
                assertTrue { result.nickname }
                assertTrue { result.membership }
            }
        }

        Scenario("opting in to voice metrics") {
            lateinit var result: PrivacySettings

            When("opting in to voice metrics") {
                result = subject.optIn(listOf(Measurement.VOICE))
            }

            Then("it should return true for voice") {
                assertTrue { result.voice }
            }

            And("false for all other metrics") {
                assertFalse { result.message }
                assertFalse { result.emoji }
                assertFalse { result.nickname }
                assertFalse { result.membership }
            }
        }

        Scenario("opting out of voice metrics") {
            lateinit var givenSubject: PrivacySettings
            lateinit var result: PrivacySettings

            Given("privacy settings with complete opt-ins") {
                givenSubject = subject.optIn(allMeasurements)
            }

            When("opting out of voice metrics") {
                result = givenSubject.optOut(listOf(Measurement.VOICE))
            }

            Then("it should return false for voice") {
                assertFalse { result.voice }
            }

            And("true for all other metrics") {
                assertTrue { result.message }
                assertTrue { result.emoji }
                assertTrue { result.nickname }
                assertTrue { result.membership }
            }
        }

        Scenario("opting in to nickname metrics") {
            lateinit var result: PrivacySettings

            When("opting in to nickname metrics") {
                result = subject.optIn(listOf(Measurement.NICKNAME))
            }

            Then("it should return true for nickname") {
                assertTrue { result.nickname }
            }

            And("false for all other metrics") {
                assertFalse { result.message }
                assertFalse { result.emoji }
                assertFalse { result.voice }
                assertFalse { result.membership }
            }
        }

        Scenario("opting out of nickname metrics") {
            lateinit var givenSubject: PrivacySettings
            lateinit var result: PrivacySettings

            Given("privacy settings with complete opt-ins") {
                givenSubject = subject.optIn(allMeasurements)
            }

            When("opting out of nickname metrics") {
                result = givenSubject.optOut(listOf(Measurement.NICKNAME))
            }

            Then("it should return false for nickname") {
                assertFalse { result.nickname }
            }

            And("true for all other metrics") {
                assertTrue { result.message }
                assertTrue { result.emoji }
                assertTrue { result.voice }
                assertTrue { result.membership }
            }
        }

        Scenario("opting in to membership metrics") {
            lateinit var result: PrivacySettings

            When("opting in to membership metrics") {
                result = subject.optIn(listOf(Measurement.MEMBERSHIP))
            }

            Then("it should return true for membership") {
                assertTrue { result.membership }
            }

            And("false for all other metrics") {
                assertFalse { result.message }
                assertFalse { result.emoji }
                assertFalse { result.voice }
                assertFalse { result.nickname }
            }
        }

        Scenario("opting out of membership metrics") {
            lateinit var givenSubject: PrivacySettings
            lateinit var result: PrivacySettings

            Given("privacy settings with complete opt-ins") {
                givenSubject = subject.optIn(allMeasurements)
            }

            When("opting out of membership metrics") {
                result = givenSubject.optOut(listOf(Measurement.MEMBERSHIP))
            }

            Then("it should return false for membership") {
                assertFalse { result.membership }
            }

            And("true for all other metrics") {
                assertTrue { result.message }
                assertTrue { result.emoji }
                assertTrue { result.voice }
                assertTrue { result.nickname }
            }
        }

        Scenario("opting in to all metrics") {
            lateinit var result: PrivacySettings

            When("opting in to all metrics") {
                result = subject.optIn(allMeasurements)
            }

            Then("it should return true for all metrics") {
                assertTrue { result.message }
                assertTrue { result.emoji }
                assertTrue { result.voice }
                assertTrue { result.nickname }
                assertTrue { result.membership }
            }
        }

        Scenario("opting out of all metrics") {
            lateinit var givenSubject: PrivacySettings
            lateinit var result: PrivacySettings

            Given("privacy settings with complete opt-ins") {
                givenSubject = subject.optIn(allMeasurements)
            }

            When("opting out of all metrics") {
                result = givenSubject.optOut(allMeasurements)
            }

            Then("it should return false for all metrics") {
                assertFalse { result.message }
                assertFalse { result.emoji }
                assertFalse { result.voice }
                assertFalse { result.nickname }
                assertFalse { result.membership }
            }
        }
    }
})

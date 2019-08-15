import Vue from 'vue'
import Vuex from 'vuex'

Vue.use(Vuex)

export const baseStore = {
    state() {
        return {
            tests: [],
            testIds: [],

            channelIds: [],
            channels: []
        }
    },
    mutations: {
        installChannel(state, newChannel) {
            const channelIndex = state.channels.findIndex( function(channel) {
                return channel.channelId === newChannel.channelId
            })
            if (channelIndex === -1) {
                state.channels.push(newChannel)
            } else {
                state.channels[channelIndex] = newChannel
            }
        },
        installChannelIds(state, channelIds) {
            state.channelIds.length = 0
            state.channels.length = 0
            for (let i in channelIds) {
                const theId = channelIds[i]
                state.channelIds.push(theId)
                state.channels.push(null)
            }
        },



        clearTests(state) {
            state.testIds.length = 0
            state.tests.length = 0
        },
        installTestIds(state, testIds) {
            for (let i in testIds) {
                state.testIds.push(testIds[i])
            }
        },
        installTest(state, test) {
            state.tests.push(test)
        },
        installTestVariable(state, variableDesc) {
            // variableDesc is { testId: id, part: variable }
            const testId = variableDesc.testId
            const variable = variableDesc.part
            const testIndex = state.tests.findIndex(function (test) {
                return test.id === testId
            })
            if (testIndex === -1) { throw `Cannot find test id ${testId} in installTestVariable` }
            state.tests[testIndex].variables.push(variable)
        },
        updateTestVariable(state, variable) {
            const testId = variable.testId
            const variableId = variable.id

            const testIndex = state.tests.findIndex(function (test) {
                return test.id === testId
            })
            if (testIndex === -1) { throw `Cannot find test id ${testId} in updateTestVariable` }

            const variableIndex = state.tests[testIndex].variables.findIndex( function (vari) {
                return vari.id === variableId
            })
            if (variableIndex === -1) { throw `Cannot find variable id ${variableId} in test ${testId} in updateTestVariable` }
            state.tests[testIndex].variables[variableIndex] = variable
        },
        installTestFixture(state, testDesc) {
            // testDesc is { testId: id, part: variable }
            const testIndex = state.tests.findIndex(function (test) {
                return test.id === testDesc.testId
            })
            if (testIndex === -1) { throw `Cannot find test id ${testDesc.id} in installTestVariable` }
            state.tests[testIndex].fixtures.push(testDesc.part)
        },
        installTestSetup(state, testDesc) {
            // testDesc is { testId: id, part: variable }
            const testIndex = state.tests.findIndex(function (test) {
                return test.id === testDesc.testId
            })
            if (testIndex === -1) { throw `Cannot find test id ${testDesc.id} in installTestVariable` }
            state.tests[testIndex].setups.push(testDesc.part)
        },
        installTestTest(state, testDesc) {
            // testDesc is { testId: id, part: variable }
            const testIndex = state.tests.findIndex(function (test) {
                return test.id === testDesc.testId
            })
            if (testIndex === -1) { throw `Cannot find test id ${testDesc.id} in installTestVariable` }
            state.tests[testIndex].tests.push(testDesc.part)
        },
        installTestTeardown(state, testDesc) {
            // testDesc is { testId: id, part: variable }
            const testIndex = state.tests.findIndex(function (test) {
                return test.id === testDesc.testId
            })
            if (testIndex === -1) { throw `Cannot find test id ${testDesc.id} in installTestVariable` }
            state.tests[testIndex].teardowns.push(testDesc.part)
        }
    },
    getters: {

    }
}

<template>
    <div class="left">

        <h2>Self Test</h2>
        <p>Verify that all back-end services are responding.</p>

        <div class="selectable" @click="selfTest()">Run</div>
        <div v-if="$store.state.log.loaded">
            Proxy responding
        </div>
        <div v-else>
            Proxy not responding
        </div>
        <div v-if="$store.state.testRunner.testCollectionsLoaded">
            Test Engine responding
        </div>
        <div v-else>
            Test Engine not responding
        </div>
        <div v-if="$store.state.testRunner.hapiIsAlive">
            HAPI responding - {{$store.state.testRunner.hapiDetails}}
        </div>
        <div v-else>
            HAPI not responding - {{$store.state.testRunner.hapiDetails}}
        </div>
        <div v-if="$store.state.testRunner.xdsIsAlive">
            XDS responding - {{$store.state.testRunner.xdsDetails}}
        </div>
        <div v-else>
            XDS not responding - {{$store.state.testRunner.xdsDetails}}
        </div>


        <h2>FHIR Toolkit structure</h2>

        <p>FHIR Tookit uses four main components: the <span class="bold">Test Engine</span> sends messages and validates
        responses, the <span class="bold">Proxy</span> acts as an intermediary between components, logs messages, and performs
        translation between MHD and XDS formats, the <span class="bold">XDS Toolkit</span> validates XDS format messages
        once they have been transformed by the Proxy, and the <span class="bold">HAPI FHIR Server</span> is used to hold
        Patient resources.</p>

        <p>Details about how this structure is used to perform testing is discussed on the Configurations page (see top menu bar).</p>

        <h2>Controls</h2>
        The <span class="bold"> Control panel</span> is on the right side of the screen:<br /><br />

        <span class="bold">Test Session</span>
        - an area to work in giving isolation from other users.  Same as XDS Toolkit. Only one test session, default,
        is supported in this release.
        <br />

        <span class="bold">Channel</span>
- a channel through the Proxy.  All traffic is routed through the Proxy which provides logs of the messages and translation. There are two
kinds of channels: FHIR - data passed without modification and MHD - translation is done between MHD and XDS formats.
        <br />

        <span class="bold">Events</span>
        - list the events for a chosen Channel.  Full details of an event can be displayed by selecting it.
        <br />

        <span class="bold">Test Collections</span>
        - There are two types of Test Collections: client and server.  Client tests are used to evaluate an
        SUT that initiates a transaction such as a Document Source. Server tests evaluate SUTs that accept transactions such as a
        Document Recipient. Each Test Collection targets a particular actor. Once a Test Collection is selected it can be returned to
        by clicking on "View".

        <h2>Patient Management</h2>
        All Document Sharing tests depend on a reference to a Patient resource. There is a Test Collection named Test Patients
        that can be used to load a small collection of Patient resources into the integrated FHIR server. This must be done before any testing
        can be performed.  These are sent to the default channel which points to the integrated HAPI FHIR server.
        This can be repeated without harm - the loading process checks and only loads what is needed.  There is no overlap between
        these Patient resources and the ones defined for Connectathon.

        To load:
        <ol>
            <li>Select Test Patients from Test Collections - the appropriate tests will display in the center and the
            default channel will be selected.</li>
            <li>Click Run All</li>
        </ol>

        <h2>Predefined Channels</h2>
        A collection of Channels comes pre-configured with the toolkit:<br /><br />

        <span class="bold">default</span>
        - leads to the integrated HAPI FHIR Server.  It performs no translation.  Logging only.
        <br /><br />

        <span class="bold">sut</span>
        - a placeholder for your System Under Test. This is used for server testing only.

        <ul>
        <li>Use the Channel Editor (Config in the Channels Control panel) to
            configure the FHIR Base Address of your System Under Test before using.</li>
        </ul>

        <span class="bold">xds</span>
        - leads to a Repository/Registry simulator in XDS Toolkit. The Channel Configuration contains the XDS Site Name.
        This is configured at system start up to point to the default__asbtsrr simulator in XDS Toolkit. The location of
        XDS Toolkit is identified in the Service Properties file.

        <ul>
        <li>This simulator must have <span class="bold">Validate Against Patient Identity Feed</span> unchecked as we do not
            send Patient Identity Feed messages to the simulator.</li>
        </ul>

        This Channel is used for validating MHD Comprehensive metadeata.
        <br /><br />

        <span class="bold">limited</span>
        - leads to a Repository/Registry simulator in XDS Toolkit.  Within the Channel Configuration, the XDS Site Name
        is configured. On my system it is default__limited which is the default Test Session and the simulator limited.

        <ul>
        <li>This simulator must have <span class="bold">Validate Against Patient Identity Feed</span> unchecked as we do not
            send Patient Identity Feed messages to the simulator.</li>
            <li>This simulator must have <span class="bold">Metadata Limited</span> checked so validation is done using the
                rules for Metadata Limited messages.</li>
        </ul>


        This Channel is used for validating MHD Minimal metadeata.

        <h2>FHIR Toolkit Configuration</h2>
        For details about configuration look in the <a href="https://github.com/usnistgov/asbestos/wiki/Configuration" target="_blank">wiki</a>.

    </div>
</template>

<script>
    export default {
        state() {
            return {

            }
        },
        methods: {
            selfTest() {
                this.$store.commit('resetLogLoaded')
                this.$store.commit('resetTestCollectionsLoaded')
                this.$store.commit('setHapiIsAlive', false)
                this.$store.commit('setXdsIsAlive', false)
                this.testTestEngine()
                this.testProxy()
                this.testHapi()
                this.testXdsToolkit()
            },
            testTestEngine() {
                this.$store.dispatch('loadTestCollectionNames')
            },
            testProxy() {
                this.$store.dispatch('loadEventSummaries',{session: 'default', channel: 'default'})
            },
            testXdsToolkit() {
                this.$store.dispatch('xdsHeartbeat')
            },
            testHapi() {
                this.$store.dispatch('hapiHeartbeat')
            },
        },
        created() {
            this.selfTest()
        },
        name: "Home"
    }
</script>

<style scoped>

</style>

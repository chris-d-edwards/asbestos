<template>
    <div class="container">
        <div class="script-header">
            Script
        </div>
        <div class="report-header">
            Report
        </div>
        <div class="script">
            <vue-json-pretty :data="script"></vue-json-pretty>
        </div>
        <div class="report">
            <vue-json-pretty :data="report"></vue-json-pretty>
        </div>
    </div>
</template>

<script>
    import VueJsonPretty from 'vue-json-pretty'
    export default {
        methods: {

        },
        props: [
            // parts representing a single action
            'script', 'report',
        ],
        components: {
            VueJsonPretty
        },
        name: "ScriptDisplay"
    }
</script>

<style scoped>
    .container {
        display: grid;
        grid-template-columns: 50% 50%;
    }
    .script-header {
        grid-column: 1;
        grid-row: 1;
        font-weight: bold;
    }
    .report-header {
        grid-column: 2;
        grid-row: 1;
        font-weight: bold;
    }
    .script {
        grid-column: 1;
        grid-row: 2;
    }
    .report {
        grid-column: 2;
        grid-row: 2;
    }

</style>

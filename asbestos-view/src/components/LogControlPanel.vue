<template>
    <div>
        <div class="control-panel-item-title" @click="manage()">Raw Events</div>
        <div v-if="!selectable" class="not-available">Select Channel</div>
    </div>
</template>

<script>

    export default {
        data() {
            return {
            }
        },
        methods: {
            manage() {
                if (this.$store.state.base.session === null)
                    return
                if (this.$store.state.base.channelId === null)
                    return
                const route = `/session/${this.$store.state.base.session}/channel/${this.$store.state.base.channelId}/logs`
                this.$router.push(route)
            },
        },
        computed: {
            selectable() {
                return this.$store.state.base.session !== null && this.$store.state.base.channelId !== null
            }
        },
        created() {

        },
        mounted() {
        },
        watch: {

        },
        name: "LogControlPanel"
    }
</script>

<style scoped>
.not-available {
    color: red;
}
</style>

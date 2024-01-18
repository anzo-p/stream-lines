<script lang="ts">
    import { setContext } from 'svelte';
    import { graphqlStore, websocketStore } from '../store/resourcesStore';

    setContext('graphql', graphqlStore);
    setContext('websocket', websocketStore);
</script>

<div>
    <slot />
</div>

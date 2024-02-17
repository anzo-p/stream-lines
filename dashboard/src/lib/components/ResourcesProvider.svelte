<script lang="ts">
    import { setContext } from 'svelte';
    import { graphqlServiceProvider, websocketServiceProvider } from '../../store/servicesStore';

    setContext('graphql', graphqlServiceProvider);
    setContext('websocket', websocketServiceProvider);
</script>

<div>
    <slot />
</div>

<template>
  <div id="app-shell">
    <header class="app-nav">
      <div class="nav-brand">Java Agent Code</div>
      <nav class="nav-links">
        <router-link to="/chat">Chat</router-link>
        <router-link to="/ingest">Ingest</router-link>
        <router-link to="/history">History</router-link>
      </nav>
    </header>
    <main class="app-main">
      <router-view />
    </main>
  </div>
</template>

<script setup lang="ts">
</script>

<style scoped>
#app-shell {
  display: flex;
  flex-direction: column;
  height: 100vh;
  overflow: hidden;
}
.app-nav {
  position: sticky;
  top: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  height: 48px;
  background: #1a1a2e;
  color: #eee;
  flex-shrink: 0;
  box-shadow: 0 1px 8px rgba(15, 23, 42, 0.16);
}
.nav-brand {
  font-weight: 700;
  font-size: 16px;
}
.nav-links {
  display: flex;
  gap: 8px;
}
.nav-links a {
  color: #aab;
  text-decoration: none;
  padding: 6px 14px;
  border-radius: 6px;
  font-size: 14px;
  transition: background 0.2s;
}
.nav-links a:hover,
.nav-links a.router-link-active {
  background: #2a2a4e;
  color: #fff;
}
.app-main {
  flex: 1;
  overflow: hidden;
  display: flex;
}
</style>

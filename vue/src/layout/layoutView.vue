<script setup>

import Aside from "@/components/Aside.vue";
import Header from "@/components/Header.vue";
</script>

<template>
    <nav>
      <!--  头部  -->
      <Header />
      <!--主体-->
      <div style="display: flex;">
        <!--侧边栏-->
        <Aside />
        <!--      内容区域-->
        <router-view style="flex: 1;" />
      </div>
    </nav>
</template>

<style scoped>

</style>
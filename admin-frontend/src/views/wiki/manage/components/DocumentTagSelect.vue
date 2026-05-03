<template>
  <el-select
    :value="value"
    :disabled="disabled"
    placeholder="请选择标签"
    clearable
    multiple
    filterable
    collapse-tags
    style="width: 100%"
    @input="$emit('input', $event)"
  >
    <el-option
      v-for="item in options"
      :key="item.tagId"
      :label="item.tagName"
      :value="item.tagId"
    >
      <span>{{ item.tagName }}</span>
      <el-tag size="mini" :style="{ marginLeft: '8px', borderColor: item.tagColor, color: item.tagColor, backgroundColor: '#fff' }">
        {{ item.tagColor }}
      </el-tag>
    </el-option>
  </el-select>
</template>

<script>
import { listTagOptions } from "@/api/wiki/tag"

export default {
  name: "DocumentTagSelect",
  props: {
    value: {
      type: Array,
      default: () => []
    },
    kbId: {
      type: [Number, String],
      default: undefined
    },
    disabled: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      options: []
    }
  },
  watch: {
    kbId: {
      immediate: true,
      handler(value) {
        if (!value) {
          this.options = []
          this.$emit("input", [])
          return
        }
        listTagOptions(value).then(response => {
          this.options = response.data || []
          const optionIds = this.options.map(item => item.tagId)
          this.$emit("input", (this.value || []).filter(item => optionIds.includes(item)))
        })
      }
    }
  }
}
</script>

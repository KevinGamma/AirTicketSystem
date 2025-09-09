<template>
  <div class="avatar-upload">
    <div class="avatar-display" @click="showUploadDialog">
      <div class="avatar-container">
        <img 
          v-if="avatarUrl" 
          :src="avatarUrl" 
          :alt="$t('profile.avatar')"
          class="avatar-image"
        />
        <div v-else class="avatar-placeholder">
          <el-icon size="40"><User /></el-icon>
        </div>
        <div class="avatar-overlay">
          <el-icon size="20"><Camera /></el-icon>
        </div>
      </div>
    </div>

    <el-dialog 
      v-model="dialogVisible" 
      :title="$t('profile.uploadAvatar')" 
      width="600px"
      :before-close="handleClose"
    >
      <div class="upload-container">
        <div v-if="!imageSrc" class="file-selector">
          <el-upload
            ref="uploadRef"
            class="upload-area"
            drag
            :auto-upload="false"
            :show-file-list="false"
            :before-upload="beforeUpload"
            :on-change="handleFileChange"
            accept="image/*"
          >
            <el-icon class="el-icon--upload"><upload-filled /></el-icon>
            <div class="el-upload__text">
              {{ $t('profile.dragImageOrClick') }}
            </div>
            <div class="el-upload__tip">
              {{ $t('profile.supportedFormats') }}
            </div>
          </el-upload>
        </div>

        <div v-else class="cropper-container">
          <div class="cropper-wrapper">
            <Cropper
              ref="cropper"
              :src="imageSrc"
              :stencil-props="stencilProps"
              class="cropper"
              @change="onChange"
            />
          </div>

          <div class="cropper-controls">
            <el-button-group>
              <el-button @click="rotateLeft">
                <el-icon><RefreshLeft /></el-icon>
              </el-button>
              <el-button @click="rotateRight">
                <el-icon><RefreshRight /></el-icon>
              </el-button>
              <el-button @click="zoomIn">
                <el-icon><ZoomIn /></el-icon>
              </el-button>
              <el-button @click="zoomOut">
                <el-icon><ZoomOut /></el-icon>
              </el-button>
            </el-button-group>
          </div>

          <div class="preview-container">
            <div class="preview-title">{{ $t('profile.preview') }}</div>
            <div class="preview-circle">
              <canvas ref="preview" width="100" height="100"></canvas>
            </div>
          </div>
        </div>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="handleClose">{{ $t('common.cancel') }}</el-button>
          <el-button v-if="imageSrc" @click="resetCropper">{{ $t('profile.reselect') }}</el-button>
          <el-button 
            type="primary" 
            @click="uploadAvatar" 
            :loading="uploading"
            :disabled="!imageSrc"
          >
            {{ $t('profile.uploadAvatar') }}
          </el-button>
        </div>
      </template>
    </el-dialog>

    <el-button 
      v-if="avatarUrl" 
      size="small" 
      type="danger" 
      @click="deleteAvatar"
      :loading="deleting"
      class="delete-button"
    >
      {{ $t('profile.deleteAvatar') }}
    </el-button>
  </div>
</template>

<script>
import { Cropper } from 'vue-advanced-cropper'
import 'vue-advanced-cropper/dist/style.css'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  User, 
  Camera, 
  UploadFilled,
  RefreshLeft,
  RefreshRight,
  ZoomIn,
  ZoomOut
} from '@element-plus/icons-vue'
import api from '@/api'

export default {
  name: 'AvatarUpload',
  components: {
    Cropper,
    User,
    Camera,
    UploadFilled,
    RefreshLeft,
    RefreshRight,
    ZoomIn,
    ZoomOut
  },
  props: {
    avatarUrl: {
      type: String,
      default: ''
    }
  },
  emits: ['avatar-updated'],
  data() {
    return {
      dialogVisible: false,
      imageSrc: '',
      uploading: false,
      deleting: false,
      coordinates: null,
      stencilProps: {
        aspectRatio: 1,
        movable: true,
        resizable: true
      }
    }
  },
  methods: {
    showUploadDialog() {
      this.dialogVisible = true
    },

    handleClose() {
      this.dialogVisible = false
      this.resetCropper()
    },

    resetCropper() {
      this.imageSrc = ''
      if (this.$refs.uploadRef) {
        this.$refs.uploadRef.clearFiles()
      }
    },

    handleFileChange(file) {
      console.log('File selected:', file)
      this.processFile(file.raw || file)
    },

    beforeUpload(file) {
      console.log('Before upload called:', file)
      return false // Prevent auto upload
    },

    processFile(file) {
      console.log('Processing file:', file)
      
      // Validate file type
      const isImage = file.type.indexOf('image/') === 0
      if (!isImage) {
        ElMessage.error(this.$t('profile.invalidFileType'))
        return false
      }

      // Validate file size (5MB)
      const isLt5M = file.size / 1024 / 1024 < 5
      if (!isLt5M) {
        ElMessage.error(this.$t('profile.fileTooLarge'))
        return false
      }

      // Read file and set image source
      const reader = new FileReader()
      reader.onload = (e) => {
        console.log('File loaded:', e.target.result.substring(0, 100) + '...')
        this.imageSrc = e.target.result
        this.$nextTick(() => {
          this.updatePreview()
        })
      }
      reader.readAsDataURL(file)
      
      return true
    },

    onChange({ coordinates }) {
      this.coordinates = coordinates
      this.updatePreview()
    },

    rotateLeft() {
      this.$refs.cropper.rotate(-90)
    },

    rotateRight() {
      this.$refs.cropper.rotate(90)
    },

    zoomIn() {
      this.$refs.cropper.zoom(1.2)
    },

    zoomOut() {
      this.$refs.cropper.zoom(0.8)
    },

    updatePreview() {
      this.$nextTick(() => {
        if (this.$refs.cropper && this.$refs.preview) {
          const canvas = this.$refs.cropper.getResult()
          if (canvas && canvas.canvas) {
            const ctx = this.$refs.preview.getContext('2d')
            ctx.clearRect(0, 0, 100, 100)
            ctx.save()
            ctx.beginPath()
            ctx.arc(50, 50, 50, 0, 2 * Math.PI)
            ctx.clip()
            ctx.drawImage(canvas.canvas, 0, 0, 100, 100)
            ctx.restore()
          }
        }
      })
    },

    async uploadAvatar() {
      if (!this.$refs.cropper) {
        return
      }

      this.uploading = true
      
      try {
        // Get cropped image data
        const result = this.$refs.cropper.getResult()
        if (!result || !result.canvas) {
          throw new Error('No cropped image available')
        }

        const blob = await new Promise((resolve) => {
          result.canvas.toBlob(resolve, 'image/png', 0.8)
        })
        
        const formData = new FormData()
        formData.append('file', blob, 'avatar.png')

        const response = await api.post('/auth/avatar', formData, {
          headers: {
            'Content-Type': 'multipart/form-data'
          }
        })
        
        if (response.data.success) {
          ElMessage.success(this.$t('profile.avatarUploadSuccess'))
          this.$emit('avatar-updated', response.data.data)
          this.handleClose()
        }
      } catch (error) {
        console.error('Upload error:', error)
        ElMessage.error(error.response?.data?.message || this.$t('profile.avatarUploadFailed'))
      } finally {
        this.uploading = false
      }
    },

    async deleteAvatar() {
      try {
        await ElMessageBox.confirm(
          this.$t('profile.confirmDeleteAvatar'),
          this.$t('profile.deleteAvatar'),
          {
            confirmButtonText: this.$t('common.confirm'),
            cancelButtonText: this.$t('common.cancel'),
            type: 'warning'
          }
        )

        this.deleting = true
        const response = await api.delete('/auth/avatar')
        
        if (response.data.success) {
          ElMessage.success(this.$t('profile.avatarDeleteSuccess'))
          this.$emit('avatar-updated', response.data.data)
        }
      } catch (error) {
        if (error !== 'cancel') {
          ElMessage.error(error.response?.data?.message || this.$t('profile.avatarDeleteFailed'))
        }
      } finally {
        this.deleting = false
      }
    }
  },

  watch: {
    imageSrc() {
      if (this.imageSrc) {
        this.$nextTick(() => {
          this.updatePreview()
        })
      }
    }
  }
}
</script>

<style scoped>
.avatar-upload {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.avatar-display {
  cursor: pointer;
  margin-bottom: 16px;
}

.avatar-container {
  position: relative;
  width: 100px;
  height: 100px;
  border-radius: 50%;
  overflow: hidden;
  border: 3px solid #e4e7ed;
  background: #f5f7fa;
  transition: all 0.3s;
}

.avatar-container:hover {
  border-color: #409eff;
}

.avatar-container:hover .avatar-overlay {
  opacity: 1;
}

.avatar-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #c0c4cc;
}

.avatar-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  opacity: 0;
  transition: opacity 0.3s;
}

.upload-container {
  min-height: 300px;
}

.file-selector {
  height: 200px;
}

.upload-area {
  width: 100%;
  height: 100%;
}

.cropper-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.cropper-wrapper {
  width: 100%;
  height: 300px;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  overflow: hidden;
}

.cropper {
  width: 100%;
  height: 100%;
}

.cropper-controls {
  display: flex;
  justify-content: center;
}

.preview-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.preview-title {
  font-size: 14px;
  color: #606266;
}

.preview-circle {
  width: 100px;
  height: 100px;
  border: 1px solid #e4e7ed;
  border-radius: 50%;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
}

.delete-button {
  margin-top: 8px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
</style>
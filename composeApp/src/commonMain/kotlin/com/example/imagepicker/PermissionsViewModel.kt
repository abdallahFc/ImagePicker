package com.example.imagepicker

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.icerock.moko.permissions.DeniedAlwaysException
import dev.icerock.moko.permissions.DeniedException
import dev.icerock.moko.permissions.Permission
import dev.icerock.moko.permissions.PermissionState
import dev.icerock.moko.permissions.PermissionsController
import dev.icerock.moko.permissions.RequestCanceledException
import dev.icerock.moko.permissions.gallery.GALLERY
import kotlinx.coroutines.launch

class GalleryPermissionViewModel(
    private val permissionsController: PermissionsController
) : ViewModel() {

    private var _uiState by mutableStateOf(GalleryUiState())
    val uiState: GalleryUiState get() = _uiState

    init {
        checkInitialPermissionState()
    }

    private fun checkInitialPermissionState() {
        viewModelScope.launch {
            try {
                _uiState = _uiState.copy(isLoading = true)
                val state = permissionsController.getPermissionState(Permission.GALLERY)
                _uiState = _uiState.copy(
                    permissionState = state,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState = _uiState.copy(
                    isLoading = false,
                    errorMessage = "Failed to check permission: ${e.message}"
                )
            }
        }
    }

    fun requestGalleryPermission() {
        viewModelScope.launch {
            try {
                _uiState = _uiState.copy(isLoading = true, errorMessage = null)
                permissionsController.providePermission(Permission.GALLERY)
                _uiState = _uiState.copy(
                    permissionState = PermissionState.Granted,
                    isLoading = false
                )
            } catch (e: DeniedAlwaysException) {
                _uiState = _uiState.copy(
                    permissionState = PermissionState.DeniedAlways,
                    isLoading = false
                )
            } catch (e: DeniedException) {
                _uiState = _uiState.copy(
                    permissionState = PermissionState.Denied,
                    isLoading = false
                )
            } catch (e: RequestCanceledException) {
                _uiState = _uiState.copy(
                    isLoading = false,
                    errorMessage = "Permission request was cancelled"
                )
            } catch (e: Exception) {
                _uiState = _uiState.copy(
                    isLoading = false,
                    errorMessage = "Error requesting permission: ${e.message}"
                )
            }
        }
    }

    fun onImageSelected(sharedImage: SharedImage?) {
        viewModelScope.launch {
            try {
                _uiState = _uiState.copy(isLoading = true, errorMessage = null)

                if (sharedImage != null) {
                    val imageBytes = sharedImage.toByteArray()
                    val imageBitmap = sharedImage.toImageBitmap()

                    _uiState = _uiState.copy(
                        selectedImage = sharedImage,
                        selectedImageBytes = imageBytes,
                        selectedImageBitmap = imageBitmap,
                        isLoading = false
                    )
                } else {
                    _uiState = _uiState.copy(
                        isLoading = false,
                        errorMessage = "Failed to load image"
                    )
                }
            } catch (e: Exception) {
                _uiState = _uiState.copy(
                    isLoading = false,
                    errorMessage = "Error processing image: ${e.message}"
                )
            }
        }
    }

    fun clearError() {
        _uiState = _uiState.copy(errorMessage = null)
    }

    fun clearSelectedImage() {
        _uiState = _uiState.copy(
            selectedImage = null,
            selectedImageBytes = null,
            selectedImageBitmap = null
        )
    }

    fun openAppSettings() {
        permissionsController.openAppSettings()
    }
}

data class GalleryUiState(
    val permissionState: PermissionState = PermissionState.NotDetermined,
    val selectedImage: SharedImage? = null,
    val selectedImageBytes: ByteArray? = null,
    val selectedImageBitmap: ImageBitmap? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as GalleryUiState
        if (permissionState != other.permissionState) return false
        if (selectedImage != other.selectedImage) return false
        if (selectedImageBytes != null) {
            if (other.selectedImageBytes == null) return false
            if (!selectedImageBytes.contentEquals(other.selectedImageBytes)) return false
        } else if (other.selectedImageBytes != null) return false
        if (selectedImageBitmap != other.selectedImageBitmap) return false
        if (isLoading != other.isLoading) return false
        if (errorMessage != other.errorMessage) return false
        return true
    }

    override fun hashCode(): Int {
        var result = permissionState.hashCode()
        result = 31 * result + (selectedImage?.hashCode() ?: 0)
        result = 31 * result + (selectedImageBytes?.contentHashCode() ?: 0)
        result = 31 * result + (selectedImageBitmap?.hashCode() ?: 0)
        result = 31 * result + isLoading.hashCode()
        result = 31 * result + (errorMessage?.hashCode() ?: 0)
        return result
    }
}
# Profile Photo Cropper Design

## Goal

Let users upload low-resolution profile photos without hitting backend minimum-size validation, while keeping the backend protected from unexpectedly large or malformed uploads.

## Current Behavior

`ProfileImageUpload` renders the selected image at its own displayed dimensions. Small images therefore produce a small cropper inside a large dialog. The crop output canvas also uses the displayed crop dimensions, so a small displayed crop can send a sub-500px image to `/api/file-storage/profile-photo`, which the backend rejects.

## Design

The frontend owns user-facing normalization. The crop dialog should present a stable square work area that is visually consistent for small and large source images. The selected image is contained inside that area and the crop starts as a centered square.

The upload payload is always rendered to a fixed `800x800` JPEG canvas, matching the backend profile detail image size. Small source images may be upscaled; this is acceptable because upload should not block normal users for low-resolution photos. Large source images are downscaled before upload so the backend never receives a huge crop.

Backend minimum validation remains in place as a developer guardrail. If a request still fails, the frontend should surface the backend message when available instead of only showing a generic upload failure.

## Testing

Add focused frontend unit tests for the pure cropper helpers:

- centered square crop calculation uses the available image bounds
- upload canvas coordinates map displayed crop pixels back to natural image pixels
- backend ProblemDetail-style errors produce a readable snackbar message

The component change is then verified through the focused test file, lint, and frontend build.

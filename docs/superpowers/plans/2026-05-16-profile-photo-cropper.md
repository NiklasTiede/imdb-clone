# Profile Photo Cropper Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Normalize profile photo crops in the frontend so users can upload small images and the backend receives a bounded `800x800` JPEG.

**Architecture:** Extract crop math and error-message helpers from `ProfileImageUpload` into a focused `profilePhotoCropper.ts` utility. Test the utility first, then wire the component to stable dialog sizing, centered initial crop, fixed upload canvas size, and backend error messages.

**Tech Stack:** React 19, Material UI 9, react-image-crop 11, Vitest.

---

### Task 1: Cropper Utility Tests

**Files:**
- Create: `frontend/src/features/account/components/profilePhotoCropper.test.ts`
- Create: `frontend/src/features/account/components/profilePhotoCropper.ts`

- [ ] Write failing tests for centered crop, canvas draw coordinates, and upload error-message extraction.
- [ ] Run `cd frontend && yarn test -- profilePhotoCropper.test.ts` and verify failures are due to missing utility exports.
- [ ] Implement the utility exports.
- [ ] Run `cd frontend && yarn test -- profilePhotoCropper.test.ts` and verify the tests pass.

### Task 2: Component Wiring

**Files:**
- Modify: `frontend/src/features/account/components/ProfileImageUpload.tsx`

- [ ] Use the cropper utility for centered initial crop and fixed-size crop rendering.
- [ ] Style the dialog body with a stable square crop stage that constrains both small and large images.
- [ ] Surface backend upload errors through the snackbar.
- [ ] Run `cd frontend && yarn test -- profilePhotoCropper.test.ts`.

### Task 3: Verification

- [ ] Run `cd frontend && yarn run lint`.
- [ ] Run `cd frontend && yarn build`.
- [ ] Commit the implementation.

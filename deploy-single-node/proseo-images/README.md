For convenience it may be useful to link the following files into this directory:
- From `../../deploy/brain/prepare_proseo/files`:
  - run_control_instance.sh
  - stop_control_instance.sh
  - stop_brain.sh
- From `../../deploy/proseo-images`:
  - build_images.sh
  - push_images.sh
  - delete_images.sh
  

To create native (ARM64) images on Macs with Apple Silicon, set
```
export PROSEO_PLATFORM=linux/arm64
```
before starting any of the scripts above.

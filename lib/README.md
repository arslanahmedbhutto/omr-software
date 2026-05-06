# External JAR Dependencies

Place manually-downloaded JAR files here that are not available on Maven Central.

## Morena (TWAIN Scanner Library)

The `ScannerController.java` is designed to work with the Morena library for real scanner integration. This is a commercial library with a free trial option.

### Download instructions:
1. Go to http://www.gnome.sk/Twain/jtp.html
2. Download the latest Morena JAR (e.g., `morena7.jar`)
3. Place it in this `lib/` folder
4. Update `pom.xml` to point to the correct filename if different

### Alternative: No scanner needed

If you don't want to deal with real scanner integration yet, the project runs in **test mode** by default:
- Create a folder named `test-images/` at the project root
- Put JPG/PNG scanned images in it
- The app will read them sequentially to simulate scanning

This is great for development and testing the OMR processing logic before wiring up hardware.

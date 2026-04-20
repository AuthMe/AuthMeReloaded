## Bat Helpers
Collection of .bat files to quickly perform some frequent development tasks.
They allow you to quickly build the project and to move the generated JAR to
the plugins folder of your test server.

### Setup
1. Copy the files into a new, convenient directory
2. Open setvars.bat with a text editor and add the correct directories
3. Open `cmd` and navigate to your _bathelpers_ folder (`cd C:\path\the\folder`)
4. Type `list_files.bat` (Hint: Type `l` and hit Tab) to see the available tasks

### Example use case
1. After writing changes, `build_project` to build project
2. `move_plugin` moves the JAR file to the plugin folder
3. `run_server` to start the server with the fresh JAR
4. Problem detected, stop the server
5. Make a small change, use `quick_build` and `move_plugin` to update
6. Verify the change again on the server: `run_server`

All files start with a different letter, so you can conveniently type the
first letter and then complete with Tab.

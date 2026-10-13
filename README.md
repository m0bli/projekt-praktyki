cd "$env:LOCALAPPDATA\Android\Sdk\platform-tools"
.\adb.exe devices
.\adb.exe reverse tcp:13308 tcp:13308

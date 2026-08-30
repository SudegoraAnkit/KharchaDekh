import os
import sys
import re
import shutil
import subprocess

def get_version_info():
    gradle_path = os.path.join("app", "build.gradle.kts")
    if not os.path.exists(gradle_path):
        print(f"Error: Gradle file not found at {gradle_path}")
        return ("unknown", "unknown")
    with open(gradle_path, "r", encoding="utf-8") as f:
        content = f.read()
    vname_match = re.search(r'versionName\s*=\s*"([^"]+)"', content)
    vcode_match = re.search(r'versionCode\s*=\s*(\d+)', content)
    vname = vname_match.group(1) if vname_match else "unknown"
    vcode = vcode_match.group(1) if vcode_match else "unknown"
    return (vname, vcode)

def run_build(store_pw, key_pw):
    print("Running Gradle assembleRelease and bundleRelease...")
    # Set environment variables for passwords
    env = os.environ.copy()
    if store_pw:
        env["STORE_PASSWORD"] = store_pw
    if key_pw:
        env["KEY_PASSWORD"] = key_pw
    
    # Run gradle command
    cmd = [os.path.join(".", "gradlew.bat" if os.name == "nt" else "gradlew"), "assembleRelease", "bundleRelease"]
    result = subprocess.run(cmd, env=env)
    return result.returncode == 0

def copy_binaries(version):
    out_dir = ".build-outputs"
    os.makedirs(out_dir, exist_ok=True)
    
    apk_src = os.path.join("app", "build", "outputs", "apk", "release", "app-release.apk")
    aab_src = os.path.join("app", "build", "outputs", "bundle", "release", "app-release.aab")
    
    apk_dest = os.path.join(out_dir, f"kd_{version}.apk")
    aab_dest = os.path.join(out_dir, f"kd_{version}.aab")
    
    copied = False
    if os.path.exists(apk_src):
        shutil.copy2(apk_src, apk_dest)
        print(f"Successfully copied APK to: {apk_dest}")
        copied = True
    else:
        print(f"Warning: APK not found at {apk_src}")
        
    if os.path.exists(aab_src):
        shutil.copy2(aab_src, aab_dest)
        print(f"Successfully copied AAB to: {aab_dest}")
        copied = True
    else:
        print(f"Warning: AAB not found at {aab_src}")
        
    return copied

def main():
    store_pw = os.getenv("STORE_PASSWORD", "password")
    key_pw = os.getenv("KEY_PASSWORD", "password")
    
    version_name, version_code = get_version_info()
    print(f"==========================================")
    print(f" Building Release Version: {version_name}")
    print(f" Version Code (Play Store): {version_code}")
    print(f"==========================================")
    
    if run_build(store_pw, key_pw):
        if copy_binaries(version_name):
            print(f"Release build and archiving for v{version_name} (code {version_code}) completed successfully!")
            sys.exit(0)
    print("Build or archiving failed.")
    sys.exit(1)

if __name__ == "__main__":
    main()

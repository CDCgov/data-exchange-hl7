import subprocess
import os
import argparse
import json

def fetch_changed_files():
    """Executes git diff to get a list of changed files from origin/main to HEAD."""
    cmd = ["git", "diff", "--name-only", "origin/main...HEAD"]
    result = subprocess.run(cmd, capture_output=True, text=True)
    if result.returncode != 0:
        raise Exception("Git command failed: " + result.stderr)
    changed_files = result.stdout.strip().split('\n')
    return changed_files

def analyze_paths(changed_files, nested_folders):
    """Analyzes file paths and categorizes them based on their presence in nested folders."""
    unique_folders = set()
    for file_path in changed_files:
        # Skip files ending with '.md'
        if file_path.endswith('.md'):
            continue
        # Skip paths that begin with '.github/'
        if file_path.startswith('.github'):
            continue
        if file_path.startswith('tools'):
            continue
        
        path_parts = file_path.split('/')
        top_level_folder = path_parts[0]
        
        if top_level_folder in nested_folders:
            if len(path_parts) > 1:
                sub_folder = os.path.join(top_level_folder, path_parts[1])
                unique_folders.add(sub_folder)
        else:
            unique_folders.add(top_level_folder)
            
    return list(unique_folders)

def get_app_version(path):
    """Retrieves the app version from the 'fortify.version' file located in the provided path."""
    fortify_version_path = os.path.join(path, "fortify.version")
    try:
        with open(fortify_version_path, "r") as file:
            return file.read().strip()
    except FileNotFoundError:
        return "Version file not found"

def main():
    parser = argparse.ArgumentParser(description="Analyze git diff file paths based on nested folders.")
    parser.add_argument('nested_folders', type=str, help='Comma-separated list of nested folders to analyze.')
    args = parser.parse_args()
    
    nested_folders = args.nested_folders.split(',')
    
    try:
        changed_files = fetch_changed_files()
        unique_folders = analyze_paths(changed_files, nested_folders)
        output = [{"path": folder, "app": get_app_version(folder)} for folder in unique_folders]
        escaped_output = json.dumps(output)
        print(json.dumps(escaped_output))
    except Exception as e:
        print(str(e))

if __name__ == '__main__':
    main()

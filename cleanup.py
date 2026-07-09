import sys
import re
import os

def remove_comments(content, ext):
    if ext in ['.kt', '.java', '.kts', '.gradle']:
        # This regex avoids matching strings
        regex = r'("(?:\\.|[^\\"])*")|(\'(?:\\.|[^\\\'])*\')|(/\*[\s\S]*?\*/)|(//.*)'
        def subst(match):
            if match.group(1): return match.group(1) # Double quoted string
            if match.group(2): return match.group(2) # Single quoted string
            return "" # It's a comment, return empty
        return re.sub(regex, subst, content)
    elif ext == '.xml':
        return re.sub(r'<!--[\s\S]*?-->', '', content)
    elif ext == '.properties':
        return re.sub(r'^\s*#.*', '', content, flags=re.MULTILINE)
    return content

files_file = "files_to_process.txt"
if os.path.exists(files_file):
    with open(files_file, "r") as f:
        files = [line.strip() for line in f if line.strip()]
    
    for file_path in files:
        if not os.path.exists(file_path): continue
        if ".idea" in file_path or "build" in file_path: continue
        ext = os.path.splitext(file_path)[1]
        try:
            with open(file_path, "r", encoding="utf-8") as f:
                content = f.read()
            new_content = remove_comments(content, ext)
            if new_content != content:
                with open(file_path, "w", encoding="utf-8", newline='') as f:
                    f.write(new_content)
        except Exception as e:
            print(f"Error processing {file_path}: {e}")

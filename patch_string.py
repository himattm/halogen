import sys

filepath = 'halogen-core/src/commonMain/kotlin/halogen/ThemeExpander.kt'
with open(filepath, 'r') as f:
    content = f.read()

search = """        chars[6] = HEX_CHARS[argb and 0xF]
        return String(chars)"""

replace = """        chars[6] = HEX_CHARS[argb and 0xF]
        return chars.concatToString()"""

if search in content:
    content = content.replace(search, replace)
    with open(filepath, 'w') as f:
        f.write(content)
    print("Success")
else:
    print("Search string not found")

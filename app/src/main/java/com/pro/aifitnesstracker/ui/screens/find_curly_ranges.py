import re

file_path = r"C:\Users\rohit\AIFitnessTracker\app\src\main\java\com\pro\aifitnesstracker\MainActivity.kt"

with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()
    lines = content.splitlines()

def find_function_range(start_idx, lines):
    brace_count = 0
    started = False
    for idx in range(start_idx, len(lines)):
        line = lines[idx]
        # Count braces
        for char in line:
            if char == '{':
                brace_count += 1
                started = True
            elif char == '}':
                brace_count -= 1
        if started and brace_count == 0:
            return idx + 1 # 1-based index
    return None

# Find all Composable member functions
composables = []
for idx, line in enumerate(lines):
    if '@Composable' in line or '@OptIn(ExperimentalMaterial3Api::class)' in lines[max(0, idx-1)]:
        # Look ahead for fun definition
        for j in range(1, 4):
            if idx + j < len(lines):
                next_line = lines[idx + j]
                fun_match = re.search(r'fun\s+(\w+)\s*\(', next_line)
                if fun_match:
                    fun_name = fun_match.group(1)
                    end_line = find_function_range(idx + j, lines)
                    composables.append({
                        'name': fun_name,
                        'start_line': idx + 1,
                        'end_line': end_line
                    })
                    break

for c in composables:
    print(f"Function: {c['name']} | Range: {c['start_line']} to {c['end_line']}")

"""Prompt template loader and renderer — loads prompts/*.txt and fills variables.

Uses safe ``str.replace()`` substitution so that JSON curly braces inside
templates (e.g. ``{"fields": [...]}``) are never interpreted as format keys.
"""

from pathlib import Path

# Prompt files directory (relative to backend-python/)
_PROMPTS_DIR = Path(__file__).resolve().parent.parent.parent / "prompts"

# Cache loaded templates: filename → raw content
_template_cache: dict[str, str] = {}


def _load_template(name: str) -> str:
    """Load a prompt template from file, caching in memory."""
    if name not in _template_cache:
        file_path = _PROMPTS_DIR / name
        if file_path.exists():
            _template_cache[name] = file_path.read_text(encoding="utf-8")
        else:
            _template_cache[name] = ""
    return _template_cache[name]


def _fill_template(template: str, **kwargs: str) -> str:
    """Safely replace ``{key}`` placeholders with values.

    Unlike ``str.format()``, this leaves all other curly-brace content
    (e.g. JSON examples) untouched.
    """
    result = template
    for key, value in kwargs.items():
        result = result.replace("{" + key + "}", value)
    return result


def reload_templates():
    """Clear the template cache so files are re-read from disk."""
    _template_cache.clear()


# ==================== Public API ====================


def build_extraction_prompt(fields_schema: str, ocr_text: str) -> str:
    """Build the field extraction prompt by filling the template.

    Args:
        fields_schema: Formatted field definitions (name, code, type per line).
        ocr_text: OCR-recognised text from the document.

    Returns:
        The filled prompt string ready for the LLM system message.
    """
    template = _load_template("field_extraction_default.txt")
    if not template:
        return (
            "你是一个商业文档信息提取助手。从以下OCR文本中提取指定字段。\n\n"
            f"字段定义：\n{fields_schema}\n\n"
            '以JSON格式返回：{"fields": [{"code": "...", "value": "...", "confidence": 0.0}]}'
        )
    return _fill_template(template, fields_schema=fields_schema, ocr_text=ocr_text)


def build_validation_prompt(fields_schema: str, extraction_results: str) -> str:
    """Build the validation prompt.

    Args:
        fields_schema: Formatted field definitions.
        extraction_results: JSON-serialised extraction results to validate.

    Returns:
        The filled prompt string for the LLM system message.
    """
    template = _load_template("validation_default.txt")
    if not template:
        return (
            "你是一个数据校验专家。校验以下提取结果。\n\n"
            f"字段定义：\n{fields_schema}\n\n"
            f"提取结果：\n{extraction_results}\n\n"
            '以JSON返回：{"validations": [{"field_code": "...", "severity": "..."}]}'
        )
    return _fill_template(template, fields_schema=fields_schema, extraction_results=extraction_results)


def build_reflection_prompt(
    field_code: str,
    field_name: str,
    current_value: str,
    confidence: float,
    ocr_context: str,
) -> str:
    """Build a reflection prompt to re-examine a low-confidence field.

    Args:
        field_code: The field's machine code.
        field_name: Human-readable field name.
        current_value: The currently extracted value.
        confidence: Confidence score (0-1) of the current extraction.
        ocr_context: Surrounding OCR text for context.

    Returns:
        Filled prompt string.
    """
    return (
        "你是一个商业文档数据修正专家。请重新审视以下字段的提取结果，"
        "判断是否需要修正。\n\n"
        "## 字段信息\n"
        f"- 字段编码: {field_code}\n"
        f"- 字段名称: {field_name}\n"
        f"- 当前提取值: {current_value}\n"
        f"- 当前置信度: {confidence}\n\n"
        "## OCR 上下文\n"
        f"{ocr_context}\n\n"
        "## 输出格式\n"
        "以JSON格式返回：\n"
        "```json\n"
        "{\n"
        f'  "field_code": "{field_code}",\n'
        '  "before_value": "修正前的值",\n'
        '  "after_value": "修正后的值",\n'
        '  "reason": "修正理由",\n'
        '  "confidence_after": 0.95\n'
        "}\n"
        "```\n\n"
        "如果当前值已经正确，after_value 与 before_value 保持一致，confidence_after 设为当前置信度。"
    )


def build_ocr_correction_prompt(ocr_text: str) -> str:
    """Build the OCR correction prompt."""
    template = _load_template("ocr_correction_default.txt")
    if not template:
        return f"请纠正以下OCR文本中的错误：\n{ocr_text}"
    return _fill_template(template, ocr_text=ocr_text)


def build_table_extraction_prompt(layout_regions: str, table_ocr_text: str) -> str:
    """Build the table extraction prompt from layout regions and OCR text."""
    template = _load_template("table_extraction_default.txt")
    if not template:
        return (
            "你是一个表格结构识别专家。提取表格数据。\n\n"
            f"版面区域：\n{layout_regions}\n\n"
            f"OCR文本：\n{table_ocr_text}"
        )
    return _fill_template(template, layout_regions=layout_regions, table_ocr_text=table_ocr_text)

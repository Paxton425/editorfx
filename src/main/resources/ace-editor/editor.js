const editor = ace.edit("editor");

//Ace Configurations
editor.setTheme("ace/theme/eclipse");
editor.session.setMode("ace/mode/javascript");

editor.setOptions({
    font: "monospace",
    fontSize: "14px",
    showPrintMargin: false,
    enableBasicAutocompletion: true,
    useSoftTabs: true,
    cursorStyle: "slim",
});
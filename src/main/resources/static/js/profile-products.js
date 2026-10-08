(function () {
    const LIST_SELECTOR = "[data-product-list]";
    const CHIPS_SELECTOR = ".product-chips";
    const DRAFT_SELECTOR = ".product-draft";
    const ADD_SELECTOR = ".product-add";
    const NOTICE_SELECTOR = ".product-list-notice";
    const REMOVE_CLASS = "product-remove";
    const CHIP_CLASS = "product-chip";
    const FIELD_NAME_ATTRIBUTE = "data-field-name";
    const CLASS_PREFIX = ".";
    const MAX_PRODUCTS = 50;
    const ENTER_KEY = "Enter";
    const DUPLICATE_MESSAGE = "Ten produkt jest już na liście.";
    const FULL_MESSAGE = "Lista może mieć najwyżej 50 produktów.";
    const REMOVE_LABEL = "Usuń";
    const CLICK = "click";
    const KEYDOWN = "keydown";
    const INPUT = "input";
    const EMPTY = "";
    const ELEMENT_LI = "li";
    const ELEMENT_INPUT = "input";
    const ELEMENT_SPAN = "span";
    const ELEMENT_BUTTON = "button";
    const INPUT_HIDDEN = "hidden";
    const BUTTON_TYPE = "button";

    document.querySelectorAll(LIST_SELECTOR).forEach(bindList);

    function bindList(list) {
        const chips = list.querySelector(CHIPS_SELECTOR);
        const draft = list.querySelector(DRAFT_SELECTOR);
        const addButton = list.querySelector(ADD_SELECTOR);
        const notice = list.querySelector(NOTICE_SELECTOR);
        const fieldName = list.getAttribute(FIELD_NAME_ATTRIBUTE);
        addButton.addEventListener(CLICK, function () {
            addProduct(chips, draft, notice, fieldName);
        });
        draft.addEventListener(KEYDOWN, function (event) {
            if (event.key === ENTER_KEY) {
                event.preventDefault();
                addProduct(chips, draft, notice, fieldName);
            }
        });
        draft.addEventListener(INPUT, function () {
            hideNotice(notice);
        });
        chips.addEventListener(CLICK, function (event) {
            const removeButton = event.target.closest(CLASS_PREFIX + REMOVE_CLASS);
            if (removeButton !== null) {
                removeButton.closest(CLASS_PREFIX + CHIP_CLASS).remove();
                hideNotice(notice);
            }
        });
    }

    function addProduct(chips, draft, notice, fieldName) {
        const name = draft.value.trim();
        if (name === EMPTY) {
            hideNotice(notice);
            return;
        }
        if (alreadyOnList(chips, name)) {
            showNotice(notice, DUPLICATE_MESSAGE);
            return;
        }
        if (chips.children.length >= MAX_PRODUCTS) {
            showNotice(notice, FULL_MESSAGE);
            return;
        }
        chips.append(chip(fieldName, name));
        draft.value = EMPTY;
        hideNotice(notice);
        draft.focus();
    }

    function alreadyOnList(chips, name) {
        const key = name.toLowerCase();
        return Array.from(chips.querySelectorAll(ELEMENT_INPUT)).some(function (input) {
            return input.value.toLowerCase() === key;
        });
    }

    function showNotice(notice, message) {
        notice.textContent = message;
        notice.hidden = false;
    }

    function hideNotice(notice) {
        notice.textContent = EMPTY;
        notice.hidden = true;
    }

    function chip(fieldName, name) {
        const item = document.createElement(ELEMENT_LI);
        item.className = CHIP_CLASS;
        const hidden = document.createElement(ELEMENT_INPUT);
        hidden.type = INPUT_HIDDEN;
        hidden.name = fieldName;
        hidden.value = name;
        const label = document.createElement(ELEMENT_SPAN);
        label.textContent = name;
        const remove = document.createElement(ELEMENT_BUTTON);
        remove.type = BUTTON_TYPE;
        remove.className = REMOVE_CLASS;
        remove.textContent = REMOVE_LABEL;
        item.append(hidden, label, remove);
        return item;
    }
}());

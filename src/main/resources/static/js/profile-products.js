(function () {
    const LIST_SELECTOR = "[data-product-list]";
    const CHIPS_SELECTOR = ".product-chips";
    const DRAFT_SELECTOR = ".product-draft";
    const ADD_SELECTOR = ".product-add";
    const NOTICE_SELECTOR = ".product-list-notice";
    const REMOVE_CLASS = "product-remove";
    const CHIP_CLASS = "product-chip";
    const FIELD_NAME_ATTRIBUTE = "data-field-name";
    const MAX_PRODUCTS = 50;
    const ENTER_KEY = "Enter";
    const DUPLICATE_MESSAGE = "Ten produkt jest już na liście.";

    document.querySelectorAll(LIST_SELECTOR).forEach(bindList);

    function bindList(list) {
        const chips = list.querySelector(CHIPS_SELECTOR);
        const draft = list.querySelector(DRAFT_SELECTOR);
        const addButton = list.querySelector(ADD_SELECTOR);
        const notice = list.querySelector(NOTICE_SELECTOR);
        const fieldName = list.getAttribute(FIELD_NAME_ATTRIBUTE);
        addButton.addEventListener("click", function () {
            addProduct(chips, draft, notice, fieldName);
        });
        draft.addEventListener("keydown", function (event) {
            if (event.key === ENTER_KEY) {
                event.preventDefault();
                addProduct(chips, draft, notice, fieldName);
            }
        });
        draft.addEventListener("input", function () {
            hideNotice(notice);
        });
        chips.addEventListener("click", function (event) {
            const removeButton = event.target.closest("." + REMOVE_CLASS);
            if (removeButton !== null) {
                removeButton.closest("." + CHIP_CLASS).remove();
                hideNotice(notice);
            }
        });
    }

    function addProduct(chips, draft, notice, fieldName) {
        const name = draft.value.trim();
        if (name === "") {
            hideNotice(notice);
            return;
        }
        if (alreadyOnList(chips, name)) {
            showNotice(notice, DUPLICATE_MESSAGE);
            return;
        }
        if (chips.children.length >= MAX_PRODUCTS) {
            hideNotice(notice);
            return;
        }
        chips.append(chip(fieldName, name));
        draft.value = "";
        hideNotice(notice);
        draft.focus();
    }

    function alreadyOnList(chips, name) {
        const key = name.toLowerCase();
        return Array.from(chips.querySelectorAll("input")).some(function (input) {
            return input.value.toLowerCase() === key;
        });
    }

    function showNotice(notice, message) {
        notice.textContent = message;
        notice.hidden = false;
    }

    function hideNotice(notice) {
        notice.textContent = "";
        notice.hidden = true;
    }

    function chip(fieldName, name) {
        const item = document.createElement("li");
        item.className = CHIP_CLASS;
        const hidden = document.createElement("input");
        hidden.type = "hidden";
        hidden.name = fieldName;
        hidden.value = name;
        const label = document.createElement("span");
        label.textContent = name;
        const remove = document.createElement("button");
        remove.type = "button";
        remove.className = REMOVE_CLASS;
        remove.textContent = "Usuń";
        item.append(hidden, label, remove);
        return item;
    }
}());

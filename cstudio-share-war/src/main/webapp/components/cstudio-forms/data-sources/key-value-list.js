CStudioForms.Datasources.KeyValueList = CStudioForms.Datasources.KeyValueList ||
    function(id, form, properties, constraints)  {
        this.id = id;
        this.form = form;
        this.properties = properties;
        this.constraints = constraints;

        return this;
    }

YAHOO.extend(CStudioForms.Datasources.KeyValueList, CStudioForms.CStudioFormDatasource, {

    getLabel: function() {
        return "Static Key Value pairs";
    },

    add: function(control) {
        var _self = this;

        var addContainerEl = null;

        if(control.addContainerEl) {
            addContainerEl = control.addContainerEl;
            control.addContainerEl = null;
            control.containerEl.removeChild(addContainerEl);
        }
        else {
            addContainerEl = document.createElement("div")
            control.containerEl.appendChild(addContainerEl);
            YAHOO.util.Dom.addClass(addContainerEl, 'cstudio-form-control-node-selector-add-container');
            control.addContainerEl = addContainerEl;

            addContainerEl.style.left = control.addButtonEl.offsetLeft + "px";
            addContainerEl.style.top = control.addButtonEl.offsetTop + 22 + "px";

            var list = this.getList();
            var showKeys = this.showKeys();
            for(var i=0; i<list.length; i++) {
                var itemEl = document.createElement("div");
                YAHOO.util.Dom.addClass(itemEl, 'cstudio-form-control-node-selector-add-container-item');
                if (showKeys) {
                    itemEl.innerHTML = list[i].key;
                } else {
                    itemEl.innerHTML = list[i].value;
                }
                addContainerEl.appendChild(itemEl);
                itemEl._value = list[i].value;
                itemEl._key = list[i].key;

                YAHOO.util.Event.on(itemEl, 'click', function() {
                    control.insertItem(this._key, this._value);
                    control._renderItems();
                }, itemEl);
            }
        }
    },

    showKeys: function() {
        var showKeys = false;
        var properties = this.properties;

        for(var i=0; i<properties.length; i++) {
            var prop = properties[i];

            if(prop.name == "showkeys") {
                if(prop.value && prop.value != "") {
                    showKeys = prop.value == 'true';
                    break;
                }
            }
        }

        return showKeys
    },

    getList: function(cb) {
        var value = [];
        var properties = this.properties;

        for(var i=0; i<properties.length; i++) {
            var prop = properties[i];

            if(prop.name == "options") {
                if(prop.value && prop.value != "") {
                    value = prop.value;
                    break;
                }
            }
        }

        if (cb != null && cb != undefined) {
            cb.success(value);
        } else {
            return value;
        }
    },

    getInterface: function() {
        return "item";
    },

    getName: function() {
        return "key-value-list";
    },

    getSupportedProperties: function() {
        return [
            { label: "Options", name: "options", type: "keyValueMap" },
            { label: "Show keys (item sel. only)", name: "showkeys", type: "boolean" }
        ];
    },

    getSupportedConstraints: function() {
        return [
            { label: "Required", name: "required", type: "boolean" }
        ];
    }

});


CStudioAuthoring.Module.moduleLoaded("cstudio-forms-controls-key-value-list", CStudioForms.Datasources.KeyValueList);
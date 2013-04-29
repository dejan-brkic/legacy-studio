CStudioForms.Controls.Dropdown = CStudioForms.Controls.Dropdown ||  
function(id, form, owner, properties, constraints, readonly)  {
	this.owner = owner;
	this.owner.registerField(this);
	this.errors = []; 
	this.properties = properties;
	this.constraints = constraints;
	this.inputEl = null;
	this.countEl = null;
	this.required = false;
	this.value = "_not-set";
	this.form = form;
	this.id = id;
	this.readonly = readonly;
	
	return this;
}

YAHOO.extend(CStudioForms.Controls.Dropdown, CStudioForms.CStudioFormField, {
    getLabel: function() {
        return "Dropdown";
    },
    
	_onChange: function(evt, obj) {
        if(obj.inputEl)
		    obj.value = obj.inputEl.value;
		
		if(obj.required) {
			if(obj.value == "") {
				obj.setError("required", "Field is Required");
				obj.renderValidation(true, false);
			}
			else {
				obj.clearError("required");
				obj.renderValidation(true, true);
			}
		}
		else {
			obj.renderValidation(false, true);
		}			

		obj.owner.notifyValidation();
		obj.form.updateModel(obj.id, obj.getValue());
	},

	
    	
	render: function(config, containerEl) {
		// we need to make the general layout of a control inherit from common
		// you should be able to override it -- but most of the time it wil be the same
		
		var datasource = null;
		var showEmptyValue = false;
		var _self = this;

			for(var i=0; i<config.properties.length; i++) {
				var prop = config.properties[i];
				
				if(prop.name == "datasource") {
					if(prop.value && prop.value != "") {
						var datasourceName = (Array.isArray(prop.value))?prop.value[0]:prop.value;
						datasource = this.form.datasourceMap[datasourceName];	
					}
				}
				
				if(prop.name == "emptyvalue"){
					showEmptyValue = eval(prop.value);
				}
				
				if(prop.name == "readonly" && prop.value == "true"){
					this.readonly = true;
				}
			}

			var keyValueList = null;

			var cb = {
				success: function(list) {
					keyValueList = list;
					var titleEl = document.createElement("span");
			  		    YAHOO.util.Dom.addClass(titleEl, 'cstudio-form-field-title');
						titleEl.innerHTML = config.title;
					
					var controlWidgetContainerEl = document.createElement("div");
					    YAHOO.util.Dom.addClass(controlWidgetContainerEl, 'cstudio-form-control-dropdown-container');
			
					var validEl = document.createElement("span");
						YAHOO.util.Dom.addClass(validEl, 'cstudio-form-control-validation');
						controlWidgetContainerEl.appendChild(validEl);
			
					var inputEl = document.createElement("select");
						_self.inputEl = inputEl;
						YAHOO.util.Dom.addClass(inputEl, 'cstudio-form-control-dropdown');
						
						if(showEmptyValue){
							var optionEl = document.createElement("option");
							optionEl.text = "";
							optionEl.value = "";
							inputEl.add(optionEl);
						}
						
						if(keyValueList){
							for(var j=0; j<keyValueList.length; j++) {
								var item = keyValueList[j];
								var optionEl = document.createElement("option");
								optionEl.text = item.value;
								optionEl.value = item.key;
								inputEl.add(optionEl);
							}
						}

						inputEl.value = (_self.value == "_not-set") ? config.defaultValue : _self.value;
						
						controlWidgetContainerEl.appendChild(inputEl);
						
						if(_self.readonly == true){
							inputEl.disabled = true;
						}
			
						YAHOO.util.Event.on(inputEl, 'focus', function(evt, context) { context.form.setFocusedField(context) }, _self);
						YAHOO.util.Event.on(inputEl, 'change', _self._onChange, _self);

					_self.renderHelp(config, controlWidgetContainerEl);
								
					var descriptionEl = document.createElement("span");
						YAHOO.util.Dom.addClass(descriptionEl, 'cstudio-form-field-description');
						descriptionEl.innerHTML = config.description;
			
					containerEl.appendChild(titleEl);
					containerEl.appendChild(controlWidgetContainerEl);
					containerEl.appendChild(descriptionEl);

                    _self._onChange(null, _self);
				}
			};

			if(datasource){
				datasource.getList(cb);
		    }
					
	},

	getValue: function() {
		return this.value;
	},
	
	setValue: function(value) {
		this.value = value;
        if(this.inputEl)
		    this.inputEl.value = value;
		this._onChange(null, this);
	},
	
	getName: function() {
		return "dropdown";
	},
	
	getSupportedProperties: function() {
		return [ 
		   	{ label: "Data Source", name: "datasource", type: "datasource:item" },
		    { label: "Allow Empty Value", name: "emptyvalue", type: "boolean" },
			{ label: "Readonly", name: "readonly", type: "boolean" }
		];
	},

	getSupportedConstraints: function() {
		return [
			{ label: "Required", name: "required", type: "boolean" }
		];
	}

});


CStudioAuthoring.Module.moduleLoaded("cstudio-forms-controls-dropdown", CStudioForms.Controls.Dropdown);
<#if args.baseUrl?exists>
	<#assign urlBase = args.baseUrl >
<#else>
	<#assign urlBase = url.context >
</#if>
<div class="cstudio-view incontextedit-view">
    <!-- style>
        .incontextedit-view {
            white-space: nowrap;
            overflow: hidden;
            width: 310px;
        }
    </style-->

    <!--h1 class="view-title">In Context Edit</h1-->
    <div class="view-caption">
    </div>

	<div class="view-block">
		<iframe id='in-context-edit-editor' frameborder='0' onload="CStudioAuthoring.InContextEdit.autoSizeIceDialog();">
		</iframe>
	</div>
	

</div>

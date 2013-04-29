<#if args.baseUrl?exists>
	<#assign urlBase = args.baseUrl >
<#else>
	<#assign urlBase = url.context >
</#if>
<div class="cstudio-view delete-view">

    <style>
        .delete-view .dependencies-listing .item-desc {
            white-space: nowrap;
            overflow: hidden;
            width: 310px;
        }
    </style>

    <h1 class="view-title">Submit for deletion</h1>
    <div class="view-caption">
        When would you like the checked item(s) to be deleted?
    </div>

    <div class="view-block">
        <div class="">
            <input id="now" type="radio" name="timing" class="asap" checked /> As soon as possible.
        </div>
        <div>
            <input id="settime" type="radio" name="timing" class="at-requested-time" /> At the requested time:

            <input id="datepicker" class="date-picker water-marked" value="Date..." default="Date..." />
            <input id="timepicker" class="time-picker water-marked" value="Time..." default="Time..." />
            <div class="timeButtonContainer" style="float:none;vertical-align:middle;display:inline-block;">
            <input id="timeIncrementButton" type="submit" value=""/>
            <input id="timeDecrementButton" type="submit" value=""/>
            </div>

            <span class=""><span id="timeZone">EST</span> (<a href="javascript:" class="scheduling-policy">Scheduling Policy</a>)</span>
        </div>
    </div>
    <div class="view-block">
        <input class="email-notify" type="checkbox" checked /> Email me when my items are deleted
    </div>

    <div class="view-square-wrp">
        <div class="head">
            <div style="margin-left:5px">Page</div>
        </div>
        <div class="body">
            <div class="dependencies-listing">
                <table class="dependencies-table">

                </table>
            </div>
        </div>
    </div>
    <div class="view-block items-feedback" style="text-align:center;margin-top:-10px">
        Dependencies must be checked before you can submit
    </div>

    <div class="action-wrapper">
        <button class="schedule-for-delete">Submit</button>
        <button class="cancel">Cancel</button>
    </div>

</div>

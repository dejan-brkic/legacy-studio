/**
 * @author spallam
 */
// Call the repository for the site profile
var json = remote.call("/api/sites/" + page.url.templateArgs.site);

var profile =
{
   title: "",
   shortName: ""
};

if (json.status == 200)
{
   // Create javascript objects from the repo response
   var obj = eval('(' + json + ')');
   if (obj)
   {
      profile = obj;
   }
}

// Call the repository to see if the user is site manager or not
var userIsSiteManager = false;
var userIsMember = false;
json = remote.call("/api/sites/" + page.url.templateArgs.site + "/memberships/" + stringUtils.urlEncode(user.name));
if (json.status == 200)
{
   var obj = eval('(' + json + ')');
   userIsMember = true;
   userIsSiteManager = obj.role == "SiteManager";
}

// Prepare the model
model.profile = profile;
model.userIsSiteManager = userIsSiteManager;
model.userIsMember = userIsMember;

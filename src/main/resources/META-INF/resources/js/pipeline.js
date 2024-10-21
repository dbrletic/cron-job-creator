const serialize_form = form => JSON.stringify(
    Array.from(new FormData(form).entries())
         .reduce((m, [ key, value ]) => Object.assign(m, { [key]: value }), {})
  );
  
  $('#piplelineRunForm').on('submit', function(event) {
    event.preventDefault();
    const data = {
      releaseBranch: document.getElementById('releaseBranch').value,
      userNameFFM: document.getElementById('userNameFFM').value,
      userPassword: document.getElementById('userPassword').value,
      groups: document.getElementById('groups').value,
      browser: document.getElementById('browser').value,
      url: document.getElementById('url').value,
      seleniumTestEmailList: document.getElementById('seleniumTestEmailList').value,
      logs: document.getElementById('logs').value,
      mvnArgs: document.getElementById('mvnArgs').value,
      pipelineRunName: document.getElementById('pipelineRunName').value
    };
    const jsonData = JSON.stringify(data);
    console.log(jsonData);
    $.ajax({
      type: 'POST',
      url: '/pipeline/tester-pipelines/startRun',
      data: jsonData,
      contentType: 'application/json',
      success: function(data) {
        $("#piplelineRunForm")[0].reset();
        $("#successMessage").show();      
        $("#successMessage").text("Pipeline Run Create: " + data);
      },
      error: function(xhr, status, error) {
        $("#successMessage").hide();     
        console.log("Error: " + error);
        $("#piplelineRunForm")[0].reset();
        $("<p>Error: " + error +"</p>").appendTo('#errorMessage');
      }
    });
  });
  
  const validation = new JustValidate('#piplelineRunForm');
  
  validation
    .addField('#releaseBranch', [
      {
        rule: 'required',
        errorMessage: 'Release Branch is required'
      }
    ])
    .addField('#userPassword', [
      {
        rule: 'required',
        errorMessage: 'User Password is required'
      }
    ])
    .addField('#groups', [
      {
        rule: 'required',
        errorMessage: 'Groups is required'
      }
    ])
    .addField('#browser', [
      {
        rule: 'required',
        errorMessage: 'Browser is required'
      }
    ])
    .addField('#url', [
      {
        rule: 'required',
        errorMessage: 'Url is required'
      }
    ])
    .addField('#seleniumTestEmailList', [
      {
        rule: 'required',
        errorMessage: 'Selenium Test Email List  is required'
      }
    ])
    .addField('#logs', [
        {
          rule: 'required',
          errorMessage: 'Logs  is required'
        }
      ])
      .addField('#mvnArgs', [
        {
          rule: 'required',
          errorMessage: 'Maven Args are required'
        }
      ])
      .addField('#pipelineRunName', [
        {
          rule: 'required',
          errorMessage: 'A Pipeline run name is required'
        }
      ])
    .addField('#userNameFFM', [
      {
        rule: 'required',
        errorMessage: 'User Name is required',
      }
    ]);
  
  /* Set the width of the side navigation to 250px */
  function openNav() {
    document.getElementById("mySidenav").style.width = "350px";
  }
  
  /* Set the width of the side navigation to 0 */
  function closeNav() {
    document.getElementById("mySidenav").style.width = "0";
  }
  

  
    /* Allows for extending of failures */
    document.addEventListener('DOMContentLoaded', function() {
      var test1ElementExpand = document.querySelector('#test1');
      var test2ElementExpand = document.querySelector('#test2');
      var test3ElementExpand = document.querySelector('#test3');
      var test4ElementExpand = document.querySelector('#test4');
      var test9ElementExpand = document.querySelector('#test9');
 
      if(test1ElementExpand){
        var test1Expand = document.getElementById('test1'); 
        test1Expand.addEventListener('click', function(e) {
          if (e.target.classList.contains('reveal-cell')){ 
             e.target.classList.toggle('expanded'); 
           } 
        });
      }
      if(test2ElementExpand){
        var test2Expand = document.getElementById('test2'); 
        test2Expand.addEventListener('click', function(e) {
          if (e.target.classList.contains('reveal-cell')){ 
             e.target.classList.toggle('expanded'); 
           } 
        });
      }
      if(test3ElementExpand){
        var test3Expand = document.getElementById('test3'); 
        test3Expand.addEventListener('click', function(e) {
          if (e.target.classList.contains('reveal-cell')){ 
             e.target.classList.toggle('expanded'); 
           } 
        });
      }
      if(test4ElementExpand){
        var test4Expand = document.getElementById('test4'); 
        test4Expand.addEventListener('click', function(e) {
          if (e.target.classList.contains('reveal-cell')){ 
             e.target.classList.toggle('expanded'); 
           } 
        });
      }
      if(test9ElementExpand){
        var test4Expand = document.getElementById('test9'); 
        test9Expand.addEventListener('click', function(e) {
          if (e.target.classList.contains('reveal-cell')){ 
             e.target.classList.toggle('expanded'); 
           } 
        });
      }
     });     
     

    

     /* Setting DataTables on all the tables */
     $(document).ready( function () {
      //Select all the <h3> elments on the page
      var h3Elements = document.querySelectorAll('h3');

      //Looping through each element to capitalize the first Letter of the table name
      h3Elements.forEach(function(h3Elment){
        h3Elment.textContent = h3Elment.textContent.charAt(0).toUpperCase() + h3Elment.textContent.slice(1);
      });

      //Setting DataTables on all the tables 
      //Have to do it the stupid way since I could not get the ' to escape correctly when creating this in java 
      var test1Element = document.querySelector('#test1');
      var test2Element = document.querySelector('#test2');
      var test3Element = document.querySelector('#test3');
      var test4Element = document.querySelector('#test4');
      var test9Element = document.querySelector('#test9');

      if(test1Element){
        var test1 = new DataTable('#test1', {
          paging: false,
          autoWidth: false, 
          columnDefs:  [
            { "width": "25%", "targets": 0 },
            { "width": "7%", "targets": 1 },
            { "width": "21%", "targets": 2 },
            { "width": "20%", "targets": 3 },
            { "width": "7%", "targets": 4 },
            { "width": "8%", "targets": 5 },
            { "width": "12%", "targets": 6 }
        ]
          } );
      }
      if(test2Element){
          var test2 = new DataTable('#test2', {
            paging: false,
          autoWidth: false, 
          columnDefs:  [
            { "width": "25%", "targets": 0 },
            { "width": "7%", "targets": 1 },
            { "width": "21%", "targets": 2 },
            { "width": "20%", "targets": 3 },
            { "width": "7%", "targets": 4 },
            { "width": "8%", "targets": 5 },
            { "width": "12%", "targets": 6 }
        ]
          } );
      }
      if(test3Element){
          var test3 = new DataTable('#test3', {
            paging: false,
          autoWidth: false, 
          columnDefs:  [
            { "width": "25%", "targets": 0 },
            { "width": "7%", "targets": 1 },
            { "width": "21%", "targets": 2 },
            { "width": "20%", "targets": 3 },
            { "width": "7%", "targets": 4 },
            { "width": "8%", "targets": 5 },
            { "width": "12%", "targets": 6 }
        ]
          } );
      }
      if(test4Element){
          var test4 = new DataTable('#test4', {
            paging: false,
          autoWidth: false, 
          columnDefs:  [
            { "width": "25%", "targets": 0 },
            { "width": "7%", "targets": 1 },
            { "width": "21%", "targets": 2 },
            { "width": "20%", "targets": 3 },
            { "width": "7%", "targets": 4 },
            { "width": "8%", "targets": 5 },
            { "width": "12%", "targets": 6 }
        ]
          } );
      }
      if(test9Element){
          var test9 = new DataTable('#test9', {
            paging: false,
          autoWidth: false, 
          columnDefs:  [
            { "width": "25%", "targets": 0 },
            { "width": "7%", "targets": 1 },
            { "width": "21%", "targets": 2 },
            { "width": "20%", "targets": 3 },
            { "width": "7%", "targets": 4 },
            { "width": "8%", "targets": 5 },
            { "width": "12%", "targets": 6 }
        ]
          } );
      }
     });
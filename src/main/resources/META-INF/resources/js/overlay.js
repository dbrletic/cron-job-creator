 // Get all elements with the class 'progress-name'
 const progressElements  = document.querySelectorAll('.progress-bar-link');

 progressElements.forEach(element => {
     element.addEventListener('click', function() {
     // Show overlay when the request starts
     document.getElementById('loadingOverlay').style.display = 'flex';

     // Initialize progress bar width
     const progressBar = document.getElementById('progressBar');
     let width = 0;
     progressBar.style.width = width + '%';

     const interval = setInterval(function() {
         // Increase width of the progress bar
         width += 2;
         progressBar.style.width = width + '%';
         
         // If the progress bar is filled resert it to 0
         if (width >= 100) {
           progressBar.style.width = 0;
         }
     }, 500); // Update the progress bar every 500ms
   });

 });
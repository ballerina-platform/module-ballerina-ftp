FROM stilliard/pure-ftpd:latest
RUN mkdir -p /home/in
COPY ./resources/content1.txt /home/in/test1.txt
COPY ./resources/content1.txt /home/in/test2.txt
COPY ./resources/content1.txt /home/in/test3.txt
COPY ./resources/content2.txt /home/in/test4.txt
RUN mkdir -p /home/in/folder1
RUN mkdir -p /home/in/folder1/subfolder1
RUN mkdir -p /home/in/childDirectory
RUN mkdir -p /home/in/child_directory
RUN mkdir -p /home/in/complexDirectory/subfolder1/subSubFolder1
RUN mkdir -p /home/in/complexDirectory/subfolder1/subSubFolder2
RUN mkdir -p /home/in/complexDirectory/subfolder2
RUN mkdir -p /home/in/complexDirectory/subfolder1/subSubFolder3
RUN touch /home/in/complexDirectory/subfolder1/subSubFolder1/content1.txt
RUN touch /home/in/complexDirectory/subfolder1/subSubFolder3/content1.txt
RUN touch /home/in/complexDirectory/subfolder1/subSubFolder3/content2.txt
RUN touch /home/in/child_directory/content1.txt
RUN touch /home/in/child_directory/content2.txt
